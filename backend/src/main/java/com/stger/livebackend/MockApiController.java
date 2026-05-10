package com.stger.livebackend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
public class MockApiController {
    private final Map<String, Object> debate = new LinkedHashMap<>();
    private final List<Map<String, Object>> users = new ArrayList<>();
    private final List<Map<String, Object>> streams = new ArrayList<>();
    private final Map<String, Map<String, Object>> debates = new HashMap<>();
    private final Map<String, String> streamDebates = new HashMap<>();
    private final List<Map<String, Object>> aiContents = new ArrayList<>();
    private final List<Map<String, Object>> voteRecords = new ArrayList<>();
    private final Map<String, Map<String, Object>> streamVotes = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> streamJudges = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> streamFlows = new HashMap<>();
    private final Map<String, Map<String, Object>> streamFlowStates = new HashMap<>();
    private final Map<String, Boolean> streamLiveStates = new HashMap<>();
    private Map<String, Object> liveSchedule = null;
    private boolean live = false;
    private boolean aiRunning = false;
    private String activeStreamId = "stream-001";

    public MockApiController() {
        debate.put("title", "如果有一个能一键消除痛苦的按钮，你会按吗？");
        debate.put("description", "这是一个关于痛苦、成长与人性选择的深度辩论");
        debate.put("leftPosition", "会按");
        debate.put("rightPosition", "不会按");

        users.add(user("u-1001", "小林", "left", 18));
        users.add(user("u-1002", "阿宁", "right", 22));
        users.add(user("u-1003", "Teague", "neutral", 9));

        streams.add(stream("stream-001", "主直播间", "https://example.com/live/main.m3u8", true));
        streams.add(stream("stream-002", "备用直播间", "https://example.com/live/backup.m3u8", false));
        Map<String, Object> defaultDebate = debateRecord("debate-001", debate);
        debates.put("debate-001", defaultDebate);
        streamDebates.put("stream-001", "debate-001");
        streamDebates.put("stream-002", "debate-001");
        streamVotes.put("stream-001", voteRecord(32, 28));
        streamVotes.put("stream-002", voteRecord(14, 20));
        streamJudges.put("stream-001", defaultJudges("stream-001"));
        streamJudges.put("stream-002", defaultJudges("stream-002"));
        streamFlows.put("stream-001", defaultFlow());
        streamFlows.put("stream-002", defaultFlow());
        streamFlowStates.put("stream-001", flowState("idle", 0, 180));
        streamFlowStates.put("stream-002", flowState("idle", 0, 180));
        streamLiveStates.put("stream-001", false);
        streamLiveStates.put("stream-002", false);

        aiContents.add(ai("ai-001", "正方认为痛苦可以被技术性消除，人应当拥有选择权。", "summary"));
        aiContents.add(ai("ai-002", "反方强调痛苦也承载成长、记忆和关系意义。", "insight"));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return ok(Map.of("status", "up", "service", "live-backend", "time", Instant.now().toString()));
    }

    @GetMapping("/api/admin/debate")
    public Map<String, Object> getDebate() {
        return new LinkedHashMap<>(debate);
    }

    @PutMapping("/api/admin/debate")
    public Map<String, Object> updateDebate(@RequestBody Map<String, Object> body) {
        debate.putAll(body);
        return new LinkedHashMap<>(debate);
    }

    @GetMapping("/api/v1/admin/debates/{debateId}")
    public Map<String, Object> getDebateById(@PathVariable String debateId) {
        return ok(debates.getOrDefault(debateId, debateRecord(debateId, debate)));
    }

    @PostMapping("/api/v1/admin/debates")
    public Map<String, Object> createDebate(@RequestBody Map<String, Object> body) {
        String id = "debate-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> record = debateRecord(id, body);
        debates.put(id, record);
        if (Boolean.TRUE.equals(body.get("isActive"))) {
            debate.clear();
            debate.putAll(body);
        }
        return ok(record);
    }

    @PutMapping("/api/v1/admin/debates/{debateId}")
    public Map<String, Object> updateDebateById(@PathVariable String debateId, @RequestBody Map<String, Object> body) {
        Map<String, Object> record = debates.computeIfAbsent(debateId, id -> debateRecord(id, body));
        record.putAll(body);
        record.put("updatedAt", Instant.now().toString());
        if (Boolean.TRUE.equals(body.get("isActive"))) {
            debate.clear();
            debate.putAll(body);
        }
        return ok(record);
    }

    @GetMapping({"/api/admin/users", "/api/v1/admin/users", "/api/admin/miniprogram/users"})
    public Map<String, Object> getUsers() {
        List<Map<String, Object>> items = users.stream().map(this::userView).toList();
        return ok(Map.of("users", items, "list", items, "total", items.size()));
    }

    @GetMapping("/api/admin/live/status")
    public Map<String, Object> liveStatus(
            @RequestParam(value = "stream_id", required = false) String streamId
    ) {
        String id = streamId(streamId);
        boolean isLive = isStreamLive(id);
        return ok(Map.of("isLive", isLive, "status", isLive ? "live" : "stopped", "streamId", id));
    }

    @PostMapping({"/api/live/control", "/api/admin/live/control", "/api/v1/admin/live/start"})
    public Map<String, Object> controlLive(@RequestBody(required = false) Map<String, Object> body) {
        String action = body == null ? "start" : String.valueOf(body.getOrDefault("action", "start"));
        String id = streamId(null, body);
        boolean nextLive = !"stop".equalsIgnoreCase(action);
        setStreamLive(id, nextLive);
        return ok(liveControlResult(id, nextLive, action));
    }

    @PostMapping("/api/admin/live/setup-and-start")
    public Map<String, Object> setupAndStartLive(@RequestBody(required = false) Map<String, Object> body) {
        String id = streamId(null, body);
        setStreamLive(id, true);
        return ok(liveControlResult(id, true, "start"));
    }

    @PostMapping({"/api/admin/live/stop", "/api/v1/admin/live/stop"})
    public Map<String, Object> stopLive(@RequestBody(required = false) Map<String, Object> body) {
        String id = streamId(null, body);
        setStreamLive(id, false);
        return ok(liveControlResult(id, false, "stop"));
    }

    @GetMapping("/api/admin/votes")
    public Map<String, Object> getVotes(@RequestParam(value = "stream_id", required = false) String streamId) {
        return ok(votesData(streamId(streamId)));
    }

    @GetMapping({"/api/votes", "/api/v1/votes"})
    public Map<String, Object> getPublicVotes(@RequestParam(value = "stream_id", required = false) String streamId) {
        return ok(votesData(streamId(streamId)));
    }

    @PutMapping("/api/admin/votes")
    public Map<String, Object> updateVotes(
            @RequestParam(value = "stream_id", required = false) String streamId,
            @RequestBody Map<String, Object> body
    ) {
        String id = streamId(streamId, body);
        Map<String, Object> votes = votesFor(id);
        votes.put("leftVotes", number(body.get("leftVotes"), number(votes.get("leftVotes"), 0)));
        votes.put("rightVotes", number(body.get("rightVotes"), number(votes.get("rightVotes"), 0)));
        votes.put("updatedAt", Instant.now().toString());
        return ok(votesData(id));
    }

    @PostMapping({"/api/admin/votes/reset", "/api/admin/live/reset-votes", "/api/v1/admin/live/reset-votes"})
    public Map<String, Object> resetVotes(
            @RequestParam(value = "stream_id", required = false) String streamId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String id = streamId(streamId, body);
        Map<String, Object> payload = body == null ? Map.of() : unwrapRequest(body);
        Object resetTo = payload.get("resetTo");
        Map<String, Object> resetPayload = new LinkedHashMap<>();
        if (resetTo instanceof Map<?, ?> map) {
            map.forEach((key, value) -> resetPayload.put(String.valueOf(key), value));
        }
        int leftVotes = number(resetPayload.get("leftVotes"), number(payload.get("leftVotes"), 0));
        int rightVotes = number(resetPayload.get("rightVotes"), number(payload.get("rightVotes"), 0));
        streamVotes.put(id, voteRecord(leftVotes, rightVotes));

        Map<String, Object> data = new LinkedHashMap<>(votesData(id));
        data.put("currentVotes", votesData(id));
        data.put("afterUpdate", votesData(id));
        return ok(data);
    }

    @PostMapping({"/api/admin/live/update-votes", "/api/v1/admin/live/update-votes"})
    public Map<String, Object> updateLiveVotes(@RequestBody Map<String, Object> body) {
        String id = streamId(null, body);
        String action = String.valueOf(body.getOrDefault("action", "set"));
        Map<String, Object> votes = votesFor(id);
        int currentLeft = number(votes.get("leftVotes"), 0);
        int currentRight = number(votes.get("rightVotes"), 0);
        int nextLeft = number(body.get("leftVotes"), 0);
        int nextRight = number(body.get("rightVotes"), 0);
        if ("add".equalsIgnoreCase(action)) {
            nextLeft += currentLeft;
            nextRight += currentRight;
        }
        votes.put("leftVotes", nextLeft);
        votes.put("rightVotes", nextRight);
        votes.put("updatedAt", Instant.now().toString());
        Map<String, Object> data = new LinkedHashMap<>(votesData(id));
        data.put("afterUpdate", votesData(id));
        data.put("currentVotes", votesData(id));
        return ok(data);
    }

    @PostMapping({"/api/user-vote", "/api/v1/user-vote"})
    public Map<String, Object> userVote(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = unwrapRequest(body);
        String id = streamId(null, payload);
        Map<String, Object> votes = votesFor(id);
        String userId = String.valueOf(payload.getOrDefault("userId", payload.getOrDefault("user_id", "mock-user")));

        if (payload.containsKey("leftVotes") || payload.containsKey("rightVotes")) {
            votes.put("leftVotes", number(votes.get("leftVotes"), 0) + number(payload.get("leftVotes"), 0));
            votes.put("rightVotes", number(votes.get("rightVotes"), 0) + number(payload.get("rightVotes"), 0));
        } else {
            String side = String.valueOf(payload.getOrDefault("side", payload.getOrDefault("position", "left")));
            int count = number(payload.get("votes"), 1);
            if ("right".equalsIgnoreCase(side) || "opposition".equalsIgnoreCase(side)) {
                votes.put("rightVotes", number(votes.get("rightVotes"), 0) + count);
            } else {
                votes.put("leftVotes", number(votes.get("leftVotes"), 0) + count);
            }
        }
        votes.put("updatedAt", Instant.now().toString());
        voteRecords.add(0, voteRecordForUser(id, userId, payload));
        return ok(votesData(id));
    }

    @GetMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
    public Map<String, Object> dashboard(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId == null || streamId.isBlank() ? activeDashboardStreamId() : streamId(streamId);
        Map<String, Object> data = new LinkedHashMap<>(votesData(id));
        boolean isLive = isStreamLive(id);
        data.put("totalUsers", users.size());
        data.put("activeUsers", isLive ? 128 : 12);
        data.put("isLive", isLive);
        data.put("streamId", id);
        data.put("aiStatus", aiRunning || isLive ? "running" : "stopped");
        data.put("debateTopic", debate);
        data.put("judges", judgesFor(id));
        data.put("debateFlow", flowFor(id));
        data.put("flowState", flowStateFor(id));
        data.put("totalComments", 8);
        data.put("totalLikes", 46);
        return ok(data);
    }

    @GetMapping({"/api/admin/streams", "/api/v1/admin/streams"})
    public Map<String, Object> getStreams() {
        List<Map<String, Object>> items = streams.stream().map(this::streamView).toList();
        return ok(Map.of("streams", items, "total", items.size()));
    }

    @PostMapping({"/api/admin/streams", "/api/v1/admin/streams"})
    public Map<String, Object> createStream(@RequestBody Map<String, Object> body) {
        Map<String, Object> stream = stream(
                "stream-" + UUID.randomUUID().toString().substring(0, 8),
                String.valueOf(body.getOrDefault("name", "新直播间")),
                String.valueOf(body.getOrDefault("url", "https://example.com/live/new.m3u8")),
                false
        );
        streams.add(stream);
        streamLiveStates.put(String.valueOf(stream.get("id")), false);
        return ok(stream);
    }

    @DeleteMapping("/api/admin/streams/{id}")
    public Map<String, Object> deleteStream(@PathVariable String id) {
        streams.removeIf(stream -> id.equals(stream.get("id")));
        return ok(Map.of("id", id));
    }

    @PutMapping({"/api/admin/streams/{id}", "/api/v1/admin/streams/{id}"})
    public Map<String, Object> updateStream(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> stream = findStream(id);
        if (stream != null) {
            stream.putAll(body);
            stream.put("updatedAt", Instant.now().toString());
        }
        return ok(stream == null ? Map.of("id", id) : stream);
    }

    @GetMapping("/api/v1/admin/streams/{streamId}/debate")
    public Map<String, Object> getStreamDebate(@PathVariable String streamId) {
        String debateId = streamDebates.get(streamId);
        Map<String, Object> record = debateId == null ? null : debates.get(debateId);
        return ok(record);
    }

    @PutMapping("/api/v1/admin/streams/{streamId}/debate")
    public Map<String, Object> associateStreamDebate(@PathVariable String streamId, @RequestBody Map<String, Object> body) {
        String debateId = String.valueOf(body.getOrDefault("debate_id", body.getOrDefault("debateId", "")));
        if (!debateId.isBlank()) {
            streamDebates.put(streamId, debateId);
            Map<String, Object> stream = findStream(streamId);
            Map<String, Object> record = debates.get(debateId);
            if (stream != null && record != null) {
                stream.put("debateId", debateId);
                stream.put("debateTitle", record.get("title"));
                stream.put("updatedAt", Instant.now().toString());
            }
            if (record != null && Boolean.TRUE.equals(record.get("isActive"))) {
                debate.clear();
                debate.putAll(record);
            }
        }
        return ok(Map.of("streamId", streamId, "debateId", debateId));
    }

    @DeleteMapping("/api/v1/admin/streams/{streamId}/debate")
    public Map<String, Object> deleteStreamDebate(@PathVariable String streamId) {
        streamDebates.remove(streamId);
        Map<String, Object> stream = findStream(streamId);
        if (stream != null) {
            stream.remove("debateId");
            stream.put("debateTitle", "");
            stream.put("updatedAt", Instant.now().toString());
        }
        return ok(Map.of("streamId", streamId));
    }

    @PostMapping("/api/admin/streams/{id}/toggle")
    public Map<String, Object> toggleStream(@PathVariable String id) {
        Map<String, Object> stream = findStream(id);
        if (stream != null) {
            boolean enabled = !Boolean.TRUE.equals(stream.get("enabled"));
            stream.put("enabled", enabled);
            stream.put("active", enabled);
            stream.put("status", enabled ? "enabled" : "disabled");
            stream.put("updatedAt", Instant.now().toString());
        }
        return ok(stream == null ? Map.of("id", id) : stream);
    }

    @GetMapping({"/api/admin/ai-content", "/api/admin/ai-content/list", "/api/v1/admin/ai-content/list"})
    public Map<String, Object> aiContentList() {
        return ok(Map.of("list", aiContents, "items", aiContents, "total", aiContents.size()));
    }

    @GetMapping({"/api/v1/ai-content", "/api/ai-content"})
    public Map<String, Object> publicAiContent(
            @RequestParam(value = "stream_id", required = false) String streamId
    ) {
        return ok(Map.of("streamId", streamId(streamId), "list", aiContents, "items", aiContents, "total", aiContents.size()));
    }

    @PostMapping("/api/admin/ai-content")
    public Map<String, Object> createAiContent(@RequestBody Map<String, Object> body) {
        Map<String, Object> content = ai(
                "ai-" + UUID.randomUUID().toString().substring(0, 8),
                String.valueOf(body.getOrDefault("content", body.getOrDefault("text", "新的 AI 识别内容"))),
                String.valueOf(body.getOrDefault("type", body.getOrDefault("side", "summary")))
        );
        aiContents.add(0, content);
        return ok(content);
    }

    @PutMapping("/api/admin/ai-content/{id}")
    public Map<String, Object> updateAiContent(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> content = findAiContent(id);
        if (content == null) {
            content = ai(id, String.valueOf(body.getOrDefault("content", body.getOrDefault("text", ""))), String.valueOf(body.getOrDefault("type", "summary")));
            aiContents.add(0, content);
        }
        if (body.containsKey("content") || body.containsKey("text")) {
            content.put("content", String.valueOf(body.getOrDefault("content", body.get("text"))));
            content.put("text", content.get("content"));
        }
        if (body.containsKey("type") || body.containsKey("side")) {
            content.put("type", String.valueOf(body.getOrDefault("type", body.get("side"))));
            content.put("side", content.get("type"));
        }
        content.put("updatedAt", Instant.now().toString());
        return ok(content);
    }

    @DeleteMapping({"/api/admin/ai-content/{id}", "/api/admin/ai/content/{id}"})
    public Map<String, Object> deleteAiContent(@PathVariable String id) {
        aiContents.removeIf(item -> id.equals(item.get("id")));
        return ok(Map.of("id", id, "deleted", true));
    }

    @PostMapping({"/api/admin/ai/start", "/api/v1/admin/ai/start"})
    public Map<String, Object> startAi() {
        aiRunning = true;
        return ok(Map.of("status", "running", "aiStatus", "running", "isRunning", true));
    }

    @PostMapping({"/api/admin/ai/stop", "/api/v1/admin/ai/stop"})
    public Map<String, Object> stopAi() {
        aiRunning = false;
        return ok(Map.of("status", "stopped", "aiStatus", "stopped", "isRunning", false));
    }

    @PostMapping({"/api/admin/ai/toggle", "/api/v1/admin/ai/toggle"})
    public Map<String, Object> toggleAi(@RequestBody(required = false) Map<String, Object> body) {
        String action = body == null ? "" : String.valueOf(body.getOrDefault("action", ""));
        String status;
        if ("pause".equalsIgnoreCase(action)) {
            aiRunning = false;
            status = "paused";
        } else if ("resume".equalsIgnoreCase(action)) {
            aiRunning = true;
            status = "running";
        } else {
            aiRunning = !aiRunning;
            status = aiRunning ? "running" : "stopped";
        }
        return ok(Map.of("status", status, "aiStatus", status, "isRunning", aiRunning));
    }

    @GetMapping({"/api/v1/debate-topic", "/api/debate-topic"})
    public Map<String, Object> getDebateTopic(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId(streamId);
        String debateId = streamDebates.getOrDefault(id, "debate-001");
        Map<String, Object> topic = new LinkedHashMap<>(debates.getOrDefault(debateId, debateRecord(debateId, debate)));
        topic.put("streamId", id);
        topic.put("leftPosition", topic.getOrDefault("leftPosition", debate.get("leftPosition")));
        topic.put("rightPosition", topic.getOrDefault("rightPosition", debate.get("rightPosition")));
        return ok(topic);
    }

    @PostMapping("/api/comment")
    public Map<String, Object> addComment(@RequestBody Map<String, Object> body) {
        String contentId = String.valueOf(body.getOrDefault("contentId", body.getOrDefault("content_id", "")));
        Map<String, Object> content = findOrCreateAiContent(contentId);
        Map<String, Object> comment = comment(
                "comment-" + UUID.randomUUID().toString().substring(0, 8),
                String.valueOf(body.getOrDefault("text", body.getOrDefault("content", "这是一条模拟评论"))),
                String.valueOf(body.getOrDefault("user", body.getOrDefault("nickname", "匿名用户"))),
                String.valueOf(body.getOrDefault("avatar", ""))
        );
        commentsFor(content).add(comment);
        return ok(comment);
    }

    @DeleteMapping("/api/comment/{commentId}")
    public Map<String, Object> deleteComment(@PathVariable String commentId, @RequestBody(required = false) Map<String, Object> body) {
        for (Map<String, Object> content : aiContents) {
            commentsFor(content).removeIf(comment -> commentId.equals(comment.get("id")));
        }
        return ok(Map.of("commentId", commentId, "deleted", true));
    }

    @PostMapping("/api/like")
    public Map<String, Object> like(@RequestBody Map<String, Object> body) {
        String contentId = String.valueOf(body.getOrDefault("contentId", body.getOrDefault("content_id", "")));
        String commentId = String.valueOf(body.getOrDefault("commentId", body.getOrDefault("comment_id", "")));
        Map<String, Object> content = findOrCreateAiContent(contentId);
        if (!commentId.isBlank() && !"null".equals(commentId)) {
            for (Map<String, Object> comment : commentsFor(content)) {
                if (commentId.equals(comment.get("id"))) {
                    comment.put("likes", number(comment.get("likes"), 0) + 1);
                    return ok(comment);
                }
            }
        }
        content.put("likes", number(content.get("likes"), 0) + 1);
        return ok(content);
    }

    @GetMapping("/api/v1/user-votes")
    public Map<String, Object> getUserVotes(
            @RequestParam(value = "stream_id", required = false) String streamId,
            @RequestParam(value = "user_id", required = false) String userId
    ) {
        String id = streamId(streamId);
        List<Map<String, Object>> records = voteRecords.stream()
                .filter(record -> id.equals(record.get("streamId")))
                .filter(record -> userId == null || userId.equals(record.get("userId")))
                .toList();
        return ok(Map.of("streamId", id, "userId", userId == null ? "" : userId, "records", records, "total", records.size()));
    }

    @GetMapping({"/api/v1/admin/votes/statistics", "/api/admin/votes/statistics"})
    public Map<String, Object> voteStatistics(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId(streamId);
        Map<String, Object> votes = votesData(id);
        return ok(Map.of(
                "streamId", id,
                "summary", votes,
                "history", voteRecords.stream().filter(record -> id.equals(record.get("streamId"))).limit(20).toList(),
                "trend", List.of(
                        Map.of("time", "10:00", "leftVotes", 12, "rightVotes", 10),
                        Map.of("time", "10:05", "leftVotes", votes.get("leftVotes"), "rightVotes", votes.get("rightVotes"))
                )
        ));
    }

    @GetMapping("/api/admin/live/schedule")
    public Map<String, Object> getLiveSchedule() {
        Map<String, Object> schedule = liveSchedule == null ? defaultSchedule(false) : liveSchedule;
        return ok(schedule);
    }

    @PostMapping("/api/admin/live/schedule")
    public Map<String, Object> saveLiveSchedule(@RequestBody Map<String, Object> body) {
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("isScheduled", true);
        schedule.put("streamId", streamId(null, body));
        schedule.put("scheduledStartTime", body.getOrDefault("scheduledStartTime", Instant.now().plusSeconds(600).toString()));
        schedule.put("scheduledEndTime", body.getOrDefault("scheduledEndTime", ""));
        schedule.put("createdAt", Instant.now().toString());
        schedule.put("updatedAt", Instant.now().toString());
        liveSchedule = schedule;
        return ok(schedule);
    }

    @PostMapping("/api/admin/live/schedule/cancel")
    public Map<String, Object> cancelLiveSchedule() {
        liveSchedule = defaultSchedule(false);
        return ok(liveSchedule);
    }

    @GetMapping("/api/admin/rtmp/urls")
    public Map<String, Object> rtmpUrls(@RequestParam(value = "room_name", required = false) String roomName) {
        String room = roomName == null || roomName.isBlank() ? "main" : roomName;
        return ok(Map.of(
                "room_name", room,
                "push_url", "rtmp://mock-live.example.com/live/" + room,
                "play_flv", "https://mock-live.example.com/live/" + room + ".flv",
                "play_hls", "https://mock-live.example.com/live/" + room + ".m3u8"
        ));
    }

    @PostMapping("/api/wechat-login")
    public Map<String, Object> wechatLogin(@RequestBody(required = false) Map<String, Object> body) {
        String id = "wx-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> user = user(id, "微信用户", "neutral", 0);
        user.put("token", "mock-token-" + id);
        user.put("openid", "mock-openid-" + id);
        return ok(user);
    }

    @GetMapping("/api/v1/admin/ai-content/{contentId}/comments")
    public Map<String, Object> getAiComments(@PathVariable String contentId) {
        Map<String, Object> content = findOrCreateAiContent(contentId);
        return ok(Map.of("contentId", contentId, "comments", commentsFor(content), "total", commentsFor(content).size()));
    }

    @PostMapping("/api/v1/admin/ai-content/{contentId}/comments")
    public Map<String, Object> addAiComment(@PathVariable String contentId, @RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<>(body);
        payload.put("contentId", contentId);
        return addComment(payload);
    }

    @DeleteMapping("/api/v1/admin/ai-content/{contentId}/comments/{commentId}")
    public Map<String, Object> deleteAiComment(@PathVariable String contentId, @PathVariable String commentId) {
        Map<String, Object> content = findOrCreateAiContent(contentId);
        commentsFor(content).removeIf(comment -> commentId.equals(comment.get("id")));
        return ok(Map.of("contentId", contentId, "commentId", commentId, "deleted", true));
    }

    @GetMapping("/api/admin/judges")
    public Map<String, Object> getJudges(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId(streamId);
        return ok(Map.of("streamId", id, "judges", judgesFor(id), "updatedAt", Instant.now().toString()));
    }

    @PostMapping("/api/admin/judges")
    public Map<String, Object> saveJudges(@RequestBody Map<String, Object> body) {
        String id = streamId(null, body);
        Object judges = body.get("judges");
        if (judges instanceof List<?> list) {
            streamJudges.put(id, normalizeJudges(list));
        }
        return ok(Map.of("streamId", id, "judges", judgesFor(id), "updatedAt", Instant.now().toString()));
    }

    @GetMapping("/api/admin/debate-flow")
    public Map<String, Object> getDebateFlow(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId(streamId);
        return ok(Map.of(
                "streamId", id,
                "flow", flowFor(id),
                "segments", flowFor(id),
                "state", resolvedFlowState(id),
                "updatedAt", Instant.now().toString()
        ));
    }

    @PostMapping("/api/admin/debate-flow")
    public Map<String, Object> saveDebateFlow(@RequestBody Map<String, Object> body) {
        String id = streamId(null, body);
        Object flow = body.containsKey("flow") ? body.get("flow") : body.get("segments");
        if (flow instanceof List<?> list) {
            streamFlows.put(id, normalizeList(list));
            streamFlowStates.put(id, flowState("idle", 0, segmentDuration(id, 0)));
        }
        return ok(Map.of("streamId", id, "flow", flowFor(id), "segments", flowFor(id), "state", resolvedFlowState(id), "updatedAt", Instant.now().toString()));
    }

    @PostMapping("/api/admin/debate-flow/control")
    public Map<String, Object> controlDebateFlow(@RequestBody Map<String, Object> body) {
        String id = streamId(null, body);
        String action = String.valueOf(body.getOrDefault("action", "start"));
        Map<String, Object> currentState = resolvedFlowState(id);
        int currentIndex = number(currentState.get("segmentIndex"), 0);
        int segmentIndex = number(body.get("segmentIndex"), number(body.get("currentSegmentIndex"), currentIndex));
        int maxIndex = Math.max(flowFor(id).size() - 1, 0);
        int remaining = number(currentState.get("remainingSeconds"), segmentDuration(id, segmentIndex));
        String status = "running";

        if ("start".equalsIgnoreCase(action)) {
            segmentIndex = Math.min(Math.max(segmentIndex, 0), maxIndex);
            remaining = segmentDuration(id, segmentIndex);
        } else if ("pause".equalsIgnoreCase(action)) {
            status = "paused";
        } else if ("resume".equalsIgnoreCase(action)) {
            status = "running";
        } else if ("reset".equalsIgnoreCase(action)) {
            status = "idle";
            segmentIndex = 0;
            remaining = segmentDuration(id, 0);
        } else if ("next".equalsIgnoreCase(action)) {
            segmentIndex = Math.min(currentIndex + 1, maxIndex);
            remaining = segmentDuration(id, segmentIndex);
        } else if ("prev".equalsIgnoreCase(action)) {
            segmentIndex = Math.max(currentIndex - 1, 0);
            remaining = segmentDuration(id, segmentIndex);
        }

        Map<String, Object> state = flowState(status, segmentIndex, remaining);
        streamFlowStates.put(id, state);
        return ok(Map.of("streamId", id, "action", action, "state", state, "timestamp", Instant.now().toString()));
    }

    @GetMapping("/api/v1/admin/live/viewers")
    public Map<String, Object> getLiveViewers(@RequestParam(value = "stream_id", required = false) String streamId) {
        String id = streamId(streamId);
        boolean isLive = isStreamLive(id);
        return ok(Map.of("streamId", id, "viewers", isLive ? 128 : 12, "onlineUsers", users));
    }

    @GetMapping("/api/v1/admin/live/broadcast-viewers")
    public Map<String, Object> getBroadcastViewers() {
        return ok(Map.of("totalViewers", live ? 256 : 24, "streams", streams.stream().map(this::streamView).toList()));
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("success", true);
        response.put("message", "success");
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> votesData(String streamId) {
        Map<String, Object> votes = votesFor(streamId);
        int leftVotes = number(votes.get("leftVotes"), 0);
        int rightVotes = number(votes.get("rightVotes"), 0);
        int total = leftVotes + rightVotes;
        int leftPercentage = total == 0 ? 50 : Math.round(leftVotes * 100f / total);
        int rightPercentage = total == 0 ? 50 : 100 - leftPercentage;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("streamId", streamId);
        data.put("leftVotes", leftVotes);
        data.put("rightVotes", rightVotes);
        data.put("totalVotes", total);
        data.put("leftPercentage", leftPercentage);
        data.put("rightPercentage", rightPercentage);
        data.put("updatedAt", votes.get("updatedAt"));
        return data;
    }

    private Map<String, Object> votesFor(String streamId) {
        return streamVotes.computeIfAbsent(streamId(streamId), id -> voteRecord(0, 0));
    }

    private Map<String, Object> voteRecord(int leftVotes, int rightVotes) {
        Map<String, Object> votes = new LinkedHashMap<>();
        votes.put("leftVotes", leftVotes);
        votes.put("rightVotes", rightVotes);
        votes.put("updatedAt", Instant.now().toString());
        return votes;
    }

    private Map<String, Object> user(String id, String nickname, String position, int votes) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("userId", id);
        user.put("nickname", nickname);
        user.put("avatar", "");
        user.put("position", position);
        user.put("votes", votes);
        user.put("online", true);
        user.put("status", "online");
        user.put("joinTime", Instant.now().toString());
        return user;
    }

    private Map<String, Object> userView(Map<String, Object> source) {
        Map<String, Object> user = new LinkedHashMap<>(source);
        user.putIfAbsent("userId", user.getOrDefault("id", ""));
        user.putIfAbsent("avatar", "");
        user.putIfAbsent("status", Boolean.TRUE.equals(user.get("online")) ? "online" : "offline");
        user.putIfAbsent("joinTime", Instant.now().toString());
        return user;
    }

    private Map<String, Object> stream(String id, String name, String url, boolean active) {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("id", id);
        stream.put("name", name);
        stream.put("url", url);
        stream.put("type", "hls");
        stream.put("enabled", active);
        stream.put("active", active);
        stream.put("status", active ? "enabled" : "disabled");
        stream.put("debateTitle", debate.get("title"));
        stream.put("createdAt", Instant.now().toString());
        stream.put("updatedAt", Instant.now().toString());
        stream.put("liveStatus", liveStatusMap(id));
        return stream;
    }

    private Map<String, Object> streamView(Map<String, Object> source) {
        Map<String, Object> stream = new LinkedHashMap<>(source);
        stream.put("liveStatus", liveStatusMap(String.valueOf(stream.get("id"))));
        return stream;
    }

    private Map<String, Object> debateRecord(String id, Map<String, Object> source) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", id);
        record.put("title", source.getOrDefault("title", "默认辩题"));
        record.put("description", source.getOrDefault("description", ""));
        record.put("leftPosition", source.getOrDefault("leftPosition", "正方"));
        record.put("rightPosition", source.getOrDefault("rightPosition", "反方"));
        record.put("isActive", source.getOrDefault("isActive", false));
        record.put("createdAt", Instant.now().toString());
        record.put("updatedAt", Instant.now().toString());
        return record;
    }

    private Map<String, Object> findStream(String id) {
        return streams.stream()
                .filter(stream -> id.equals(stream.get("id")))
                .findFirst()
                .orElse(null);
    }

    private String streamUrl(String id) {
        Map<String, Object> stream = findStream(id);
        return stream == null ? "https://example.com/live/main.m3u8" : String.valueOf(stream.get("url"));
    }

    private boolean isStreamLive(String id) {
        return Boolean.TRUE.equals(streamLiveStates.get(streamId(id)));
    }

    private String activeDashboardStreamId() {
        if (live && activeStreamId != null && !activeStreamId.isBlank()) {
            return activeStreamId;
        }
        return "stream-001";
    }

    private void setStreamLive(String id, boolean isLive) {
        String streamId = streamId(id);
        streamLiveStates.put(streamId, isLive);
        if (isLive) {
            activeStreamId = streamId;
        }
        live = streamLiveStates.values().stream().anyMatch(Boolean.TRUE::equals);
    }

    private Map<String, Object> liveStatusMap(String id) {
        boolean isLive = isStreamLive(id);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("isLive", isLive);
        status.put("status", isLive ? "live" : "stopped");
        status.put("startTime", isLive ? Instant.now().toString() : "");
        return status;
    }

    private Map<String, Object> liveControlResult(String id, boolean isLive, String action) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("isLive", isLive);
        result.put("status", isLive ? "started" : "stopped");
        result.put("action", action);
        result.put("streamId", id);
        result.put("streamUrl", streamUrl(id));
        result.put("startTime", isLive ? Instant.now().toString() : "");
        return result;
    }

    private Map<String, Object> ai(String id, String content, String type) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("content", content);
        item.put("text", content);
        item.put("type", type);
        item.put("side", type);
        item.put("createdAt", Instant.now().toString());
        item.put("updatedAt", Instant.now().toString());
        item.put("likes", 0);
        item.put("comments", new ArrayList<Map<String, Object>>());
        return item;
    }

    private Map<String, Object> findAiContent(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return aiContents.stream()
                .filter(item -> id.equals(item.get("id")))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> findOrCreateAiContent(String id) {
        Map<String, Object> content = findAiContent(id);
        if (content != null) {
            return content;
        }
        String contentId = id == null || id.isBlank() ? "ai-" + UUID.randomUUID().toString().substring(0, 8) : id;
        content = ai(contentId, "模拟 AI 内容：" + contentId, "summary");
        aiContents.add(0, content);
        return content;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> commentsFor(Map<String, Object> content) {
        Object comments = content.get("comments");
        if (comments instanceof List<?> list) {
            boolean allMaps = list.stream().allMatch(item -> item instanceof Map<?, ?>);
            if (allMaps && comments instanceof ArrayList<?>) {
                return (List<Map<String, Object>>) comments;
            }
            List<Map<String, Object>> normalized = normalizeList(list);
            content.put("comments", normalized);
            return normalized;
        }
        List<Map<String, Object>> empty = new ArrayList<>();
        content.put("comments", empty);
        return empty;
    }

    private Map<String, Object> comment(String id, String text, String user, String avatar) {
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("id", id);
        comment.put("text", text);
        comment.put("user", user);
        comment.put("avatar", avatar);
        comment.put("likes", 0);
        comment.put("time", Instant.now().toString());
        comment.put("createdAt", Instant.now().toString());
        return comment;
    }

    private Map<String, Object> voteRecordForUser(String streamId, String userId, Map<String, Object> payload) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", "vote-" + UUID.randomUUID().toString().substring(0, 8));
        record.put("streamId", streamId);
        record.put("userId", userId);
        record.put("side", payload.getOrDefault("side", payload.getOrDefault("position", "")));
        record.put("leftVotes", number(payload.get("leftVotes"), 0));
        record.put("rightVotes", number(payload.get("rightVotes"), 0));
        record.put("votes", number(payload.get("votes"), 0));
        record.put("createdAt", Instant.now().toString());
        return record;
    }

    private Map<String, Object> defaultSchedule(boolean scheduled) {
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("isScheduled", scheduled);
        schedule.put("streamId", activeStreamId);
        schedule.put("scheduledStartTime", "");
        schedule.put("scheduledEndTime", "");
        schedule.put("updatedAt", Instant.now().toString());
        return schedule;
    }

    private List<Map<String, Object>> judgesFor(String streamId) {
        return streamJudges.computeIfAbsent(streamId(streamId), this::defaultJudges);
    }

    private List<Map<String, Object>> defaultJudges(String streamId) {
        return new ArrayList<>(List.of(
                judge("judge-1", "张教授", "主评委", 60, 40),
                judge("judge-2", "李老师", "嘉宾评委", 50, 50),
                judge("judge-3", "王专家", "嘉宾评委", 40, 60)
        ));
    }

    private Map<String, Object> judge(String id, String name, String role, int leftVotes, int rightVotes) {
        Map<String, Object> judge = new LinkedHashMap<>();
        judge.put("id", id);
        judge.put("name", name);
        judge.put("role", role);
        judge.put("avatar", "");
        judge.put("votes", leftVotes + rightVotes);
        judge.put("leftVotes", leftVotes);
        judge.put("rightVotes", rightVotes);
        return judge;
    }

    private List<Map<String, Object>> flowFor(String streamId) {
        return streamFlows.computeIfAbsent(streamId(streamId), id -> defaultFlow());
    }

    private List<Map<String, Object>> defaultFlow() {
        List<Map<String, Object>> flow = new ArrayList<>();
        flow.add(flowSegment("正方发言", 180, "left", 1));
        flow.add(flowSegment("反方质询", 120, "right", 2));
        flow.add(flowSegment("自由辩论", 240, "both", 3));
        flow.add(flowSegment("总结陈词", 120, "both", 4));
        return flow;
    }

    private Map<String, Object> flowSegment(String name, int duration, String side, int order) {
        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("name", name);
        segment.put("duration", duration);
        segment.put("side", side);
        segment.put("order", order);
        return segment;
    }

    private Map<String, Object> flowStateFor(String streamId) {
        String id = streamId(streamId);
        return streamFlowStates.computeIfAbsent(id, ignored -> flowState("idle", 0, segmentDuration(id, 0)));
    }

    private Map<String, Object> flowState(String status, int segmentIndex, int remainingSeconds) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", status);
        state.put("segmentIndex", segmentIndex);
        state.put("remainingSeconds", Math.max(0, remainingSeconds));
        state.put("startedAt", "running".equals(status) ? Instant.now().toString() : "");
        state.put("updatedAt", Instant.now().toString());
        return state;
    }

    private Map<String, Object> resolvedFlowState(String streamId) {
        String id = streamId(streamId);
        Map<String, Object> state = new LinkedHashMap<>(flowStateFor(id));
        if ("running".equals(state.get("status"))) {
            int remaining = number(state.get("remainingSeconds"), segmentDuration(id, number(state.get("segmentIndex"), 0)));
            String startedAt = String.valueOf(state.getOrDefault("startedAt", ""));
            try {
                long elapsed = Duration.between(Instant.parse(startedAt), Instant.now()).getSeconds();
                remaining = Math.max(0, remaining - (int) elapsed);
            } catch (Exception ignored) {
                // keep stored remaining seconds
            }
            state.put("remainingSeconds", remaining);
            if (remaining <= 0) {
                state.put("status", "paused");
            }
        }
        return state;
    }

    private int segmentDuration(String streamId, int segmentIndex) {
        List<Map<String, Object>> flow = flowFor(streamId);
        if (flow.isEmpty()) {
            return 0;
        }
        int index = Math.min(Math.max(segmentIndex, 0), flow.size() - 1);
        return number(flow.get(index).get("duration"), 0);
    }

    private List<Map<String, Object>> normalizeList(List<?> list) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalizedItem = new LinkedHashMap<>();
                map.forEach((key, value) -> normalizedItem.put(String.valueOf(key), value));
                normalized.add(normalizedItem);
            }
        }
        return normalized;
    }

    private List<Map<String, Object>> normalizeJudges(List<?> list) {
        List<Map<String, Object>> judges = normalizeList(list);
        for (int i = 0; i < judges.size(); i++) {
            Map<String, Object> judge = judges.get(i);
            judge.putIfAbsent("id", "judge-" + (i + 1));
            judge.putIfAbsent("name", "评委" + (i + 1));
            judge.putIfAbsent("role", i == 0 ? "主评委" : "嘉宾评委");
            judge.putIfAbsent("avatar", "");
            int votes = number(judge.get("votes"), number(judge.get("leftVotes"), 0) + number(judge.get("rightVotes"), 0));
            judge.put("votes", votes);
            judge.putIfAbsent("leftVotes", votes);
            judge.putIfAbsent("rightVotes", 0);
        }
        return judges;
    }

    private Map<String, Object> unwrapRequest(Map<String, Object> body) {
        Object request = body.get("request");
        if (request instanceof Map<?, ?> map) {
            Map<String, Object> unwrapped = new LinkedHashMap<>();
            map.forEach((key, value) -> unwrapped.put(String.valueOf(key), value));
            return unwrapped;
        }
        return body;
    }

    private String streamId(String streamId) {
        return streamId == null || streamId.isBlank() ? "stream-001" : streamId;
    }

    private String streamId(String queryStreamId, Map<String, Object> body) {
        Map<String, Object> payload = body == null ? Map.of() : unwrapRequest(body);
        Object value = payload.getOrDefault("streamId", payload.getOrDefault("stream_id", queryStreamId));
        return streamId(value == null ? null : String.valueOf(value));
    }

    private int number(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
