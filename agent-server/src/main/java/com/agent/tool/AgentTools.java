package com.agent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agent.config.AgentProperties;
import com.agent.entity.UserKnowledgeConfig;
import com.agent.mapper.UserKnowledgeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 工具集，对标 Python {@code langchain_agent.py} 中 7 个工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] WEEKDAY_CN =
            {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final int HTTP_TIMEOUT_MS = 8_000;

    private static final Map<Integer, String> WMO_ZH = Map.ofEntries(
            Map.entry(0, "晴"),
            Map.entry(1, "大部晴朗"),
            Map.entry(2, "局部多云"),
            Map.entry(3, "多云"),
            Map.entry(45, "雾"),
            Map.entry(48, "雾凇"),
            Map.entry(51, "小毛毛雨"),
            Map.entry(53, "毛毛雨"),
            Map.entry(55, "大毛毛雨"),
            Map.entry(61, "小雨"),
            Map.entry(63, "中雨"),
            Map.entry(65, "大雨"),
            Map.entry(71, "小雪"),
            Map.entry(73, "中雪"),
            Map.entry(75, "大雪"),
            Map.entry(80, "小阵雨"),
            Map.entry(81, "中阵雨"),
            Map.entry(82, "大阵雨"),
            Map.entry(95, "雷暴")
    );

    private final AgentProperties agentProperties;
    private final UserKnowledgeConfigMapper userKnowledgeConfigMapper;

    @Tool(description = "获取当前日期、时间和星期几。用户问几点、今天几号、星期几时必须调用，并原样引用工具返回的星期。")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        String weekday = WEEKDAY_CN[now.getDayOfWeek().getValue() - 1];
        return "当前时间：" + now.format(TIME_FORMAT)
                + "，今天是" + now.getYear() + "年" + now.getMonthValue() + "月" + now.getDayOfMonth() + "日，"
                + weekday + "。（星期几以本工具结果为准：" + weekday + "）";
    }

    @Tool(description = "执行基础四则运算（加减乘除）。operation 为 add/subtract/multiply/divide。")
    public String calculate(
            @ToolParam(description = "运算类型：add 加、subtract 减、multiply 乘、divide 除") String operation,
            @ToolParam(description = "第一个操作数") double a,
            @ToolParam(description = "第二个操作数") double b) {
        if (StrUtil.isBlank(operation)) {
            return "不支持的运算: " + operation;
        }
        return switch (operation.trim().toLowerCase()) {
            case "add" -> String.valueOf(NumberUtil.add(a, b));
            case "subtract" -> String.valueOf(NumberUtil.sub(a, b));
            case "multiply" -> String.valueOf(NumberUtil.mul(a, b));
            case "divide" -> {
                if (b == 0) {
                    yield "错误：除数不能为 0";
                }
                yield String.valueOf(NumberUtil.div(a, b, 10));
            }
            default -> "不支持的运算: " + operation;
        };
    }

    @Tool(description = "读取项目内文本文件。path 为相对项目根的路径，如 doc/README.md。")
    public String readProjectFile(
            @ToolParam(description = "相对项目根的文件路径") String path) {
        Path resolved = resolveProjectPath(path);
        if (resolved == null) {
            return "错误：不允许访问项目外路径";
        }
        if (!FileUtil.exist(resolved.toFile())) {
            return "错误：文件不存在 " + path;
        }
        if (!FileUtil.isFile(resolved.toFile())) {
            return "错误：不是文件 " + path;
        }
        String content = FileUtil.readUtf8String(resolved.toFile());
        if (content.length() > 8000) {
            return content.substring(0, 8000);
        }
        return content;
    }

    @Tool(description = "列出项目内目录下的文件名。path 为相对项目根的路径，如 doc。不能用于用户本机个人知识库路径。")
    public String listProjectFiles(
            @ToolParam(description = "相对项目根的目录路径，默认 .") String path) {
        String dirPath = StrUtil.isBlank(path) ? "." : path.trim();
        Path resolved = resolveProjectPath(dirPath);
        if (resolved == null) {
            return "错误：不允许访问项目外路径";
        }
        File dir = resolved.toFile();
        if (!dir.exists()) {
            return "错误：目录不存在 " + dirPath;
        }
        if (!dir.isDirectory()) {
            return "错误：不是目录 " + dirPath;
        }
        List<String> names = FileUtil.listFileNames(dir.getAbsolutePath());
        if (names.isEmpty()) {
            return "（空目录）";
        }
        return String.join(", ", names);
    }

    @Tool(description = "列出当前用户在 KNOWLEDGE 页配置的知识库目录中的 .md 文档。用户问知识库有哪些文件时必须调用。")
    public String listKnowledgeFiles() {
        Long userId = AgentToolContext.currentUserId();
        if (userId == null) {
            return "无法识别当前用户，请重新登录后再试。";
        }
        UserKnowledgeConfig config = userKnowledgeConfigMapper.selectById(userId);
        if (config == null || StrUtil.isBlank(config.getKnowledgeDir())) {
            return "尚未配置知识库。请在 KNOWLEDGE 页面设置本机 md 目录并点击 REBUILD INDEX。";
        }

        File directory = new File(config.getKnowledgeDir().trim());
        if (!directory.isDirectory()) {
            return "知识库目录不存在或不可访问：" + config.getKnowledgeDir();
        }

        List<File> mdFiles = FileUtil.loopFiles(directory, file -> StrUtil.endWithIgnoreCase(file.getName(), ".md"));
        mdFiles.sort(Comparator.comparing(File::getPath));

        List<String> lines = new ArrayList<>();
        lines.add("[知识库] " + directory.getAbsolutePath());
        if (mdFiles.isEmpty()) {
            lines.add("  （无 .md 文件，当前 RAG 仅索引 .md）");
        } else {
            int limit = Math.min(mdFiles.size(), 80);
            for (int i = 0; i < limit; i++) {
                File md = mdFiles.get(i);
                lines.add("  - " + directory.toPath().relativize(md.toPath()).toString().replace('\\', '/'));
            }
            if (mdFiles.size() > 80) {
                lines.add("  … 另有 " + (mdFiles.size() - 80) + " 个 .md 文件");
            }
        }
        lines.add("");
        lines.add("提示：仅索引 .md 文件；配置或修改后请在 KNOWLEDGE 页点击 REBUILD INDEX。");
        return String.join("\n", lines);
    }

    @Tool(description = "从用户配置的知识库检索文档片段。问笔记、文档内容时优先使用。")
    public String queryKnowledgeBase(
            @ToolParam(description = "检索问题") String question) {
        Long userId = AgentToolContext.currentUserId();
        if (userId == null) {
            return "无法识别当前用户，请重新登录后再试。";
        }
        UserKnowledgeConfig config = userKnowledgeConfigMapper.selectById(userId);
        if (config == null || StrUtil.isBlank(config.getKnowledgeDir())) {
            return "尚未配置知识库。请在 KNOWLEDGE 页面设置本机 md 目录并点击 REBUILD INDEX。";
        }
        if (config.getLastIndexedAt() == null || config.getChunkCount() == null || config.getChunkCount() <= 0) {
            return "知识库尚未完成索引。请在 KNOWLEDGE 页配置目录后点击 REBUILD INDEX，索引完成后可检索文档内容。";
        }
        // v0.7 将实现向量检索；此处返回占位提示，避免模型编造内容。
        return "向量检索功能将在下一版本（v0.7）启用。当前请使用 listKnowledgeFiles 查看可用文档，"
                + "或直接在 KNOWLEDGE 页 REBUILD 后等待 v0.7 升级。你的问题是：「"
                + StrUtil.trim(question) + "」";
    }

    @Tool(description = "查询中国城市今日天气。用户问天气时必须传入 city（如北京、上海）；未提供城市时请先询问用户。")
    public String getTodayWeather(
            @ToolParam(description = "中国城市名称，如北京、上海、深圳") String city) {
        if (StrUtil.isBlank(city)) {
            return "请提供中国城市名称后再查询，例如：北京、上海、深圳。";
        }
        try {
            String geoBody = HttpUtil.get(GEO_URL, Map.of(
                    "name", city.trim(),
                    "count", "5",
                    "language", "zh",
                    "countryCode", "CN"
            ), HTTP_TIMEOUT_MS);
            JSONObject geoJson = JSONUtil.parseObj(geoBody);
            JSONArray results = geoJson.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return "未找到中国城市「" + city.trim()
                        + "」。请使用标准城市名（如北京、成都），暂仅支持中国大陆城市。";
            }

            JSONObject loc = results.getJSONObject(0);
            double lat = loc.getDouble("latitude");
            double lon = loc.getDouble("longitude");
            String place = loc.getStr("name", city.trim());
            String admin1 = loc.getStr("admin1", "");
            String locationLabel = StrUtil.isNotBlank(admin1) && !admin1.equals(place)
                    ? place + "（" + admin1 + "）"
                    : place;

            String fcBody = HttpUtil.get(FORECAST_URL, Map.of(
                    "latitude", String.valueOf(lat),
                    "longitude", String.valueOf(lon),
                    "daily", "weather_code,temperature_2m_max,temperature_2m_min,"
                            + "precipitation_probability_max,wind_speed_10m_max",
                    "timezone", "Asia/Shanghai",
                    "forecast_days", "1"
            ), HTTP_TIMEOUT_MS);
            JSONObject fcJson = JSONUtil.parseObj(fcBody);
            JSONObject daily = fcJson.getJSONObject("daily");
            if (daily == null) {
                return "天气服务未返回「" + locationLabel + "」的数据，请稍后重试。";
            }
            JSONArray dates = daily.getJSONArray("time");
            if (dates == null || dates.isEmpty()) {
                return "天气服务未返回「" + locationLabel + "」的数据，请稍后重试。";
            }

            String dateStr = dates.getStr(0);
            int code = daily.getJSONArray("weather_code").getInt(0);
            double tMax = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double tMin = daily.getJSONArray("temperature_2m_min").getDouble(0);
            Integer pop = daily.getJSONArray("precipitation_probability_max").getInt(0);
            Double wind = daily.getJSONArray("wind_speed_10m_max").getDouble(0);
            String desc = WMO_ZH.getOrDefault(code, "天气码" + code);

            List<String> lines = new ArrayList<>();
            lines.add("【今日天气 | " + locationLabel + " | " + dateStr + "】");
            lines.add("天气：" + desc);
            lines.add(String.format("气温：%.0f°C ~ %.0f°C", tMin, tMax));
            if (pop != null) {
                lines.add("降水概率：" + pop + "%");
            }
            if (wind != null) {
                lines.add(String.format("最大风速：%.0f km/h", wind));
            }
            lines.add("（以上数据来自天气服务，请原样告知用户，勿编造数值）");
            return String.join("\n", lines);
        } catch (Exception ex) {
            log.warn("Weather tool failed for city={}", city, ex);
            return "天气服务请求失败，请稍后重试。";
        }
    }

    /** 解析并校验路径位于项目根内。 */
    private Path resolveProjectPath(String relativePath) {
        Path root = agentProperties.resolvedProjectRoot();
        Path target = root.resolve(relativePath.trim()).normalize();
        if (!target.startsWith(root)) {
            return null;
        }
        return target;
    }
}
