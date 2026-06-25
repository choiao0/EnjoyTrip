package com.enjoytrip.service;

import com.enjoytrip.config.AppProperties;
import com.enjoytrip.model.Lodging;
import com.enjoytrip.model.MapPoint;
import com.enjoytrip.model.Plan;
import com.enjoytrip.model.PlanItem;
import com.enjoytrip.model.RouteLeg;
import com.enjoytrip.model.RouteResult;
import com.enjoytrip.model.RouteStop;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteService {
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RouteService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public RouteResult recommend(Plan plan) {
        Lodging lodging = plan.getLodging();
        if (lodging == null || lodging.getLat() == 0 || lodging.getLng() == 0) {
            throw new IllegalArgumentException("숙소 좌표를 먼저 확인한 뒤 계획을 저장하세요.");
        }
        if (plan.getItems() == null || plan.getItems().isEmpty()) {
            throw new IllegalArgumentException("경로 추천을 위해 여행지 1개 이상이 필요합니다.");
        }
        if (plan.getItems().size() > appProperties.getRouteMaxWaypoints()) {
            throw new IllegalArgumentException("경유지는 최대 " + appProperties.getRouteMaxWaypoints() + "개까지 추천할 수 있습니다.");
        }

        List<RouteStop> nodes = buildNodes(plan);
        Map<String, TravelEdge> directEdges = new HashMap<>();
        double[][] weights = new double[nodes.size()][nodes.size()];
        for (int i = 0; i < nodes.size(); i += 1) {
            for (int j = 0; j < nodes.size(); j += 1) {
                if (i == j) {
                    weights[i][j] = 0;
                    continue;
                }
                TravelEdge edge = fetchEdge(nodes.get(i), nodes.get(j));
                directEdges.put(key(i, j), edge);
                weights[i][j] = edge.durationSec;
            }
        }

        double[][] shortestMatrix = buildShortestMatrix(weights);
        List<Integer> waypoints = new ArrayList<>();
        for (int i = 1; i < nodes.size(); i += 1) {
            waypoints.add(i);
        }

        PermutationResult best = new PermutationResult(Double.MAX_VALUE, new ArrayList<>());
        permute(waypoints, 0, nodes, shortestMatrix, best);

        List<Integer> sequence = new ArrayList<>();
        sequence.add(0);
        sequence.addAll(best.order);
        sequence.add(0);

        RouteResult result = new RouteResult();
        result.setPlanId(plan.getId());
        List<RouteStop> orderedStops = new ArrayList<>();
        for (Integer index : sequence) {
            orderedStops.add(nodes.get(index));
        }
        result.setOrderedStops(orderedStops);

        List<RouteLeg> legs = new ArrayList<>();
        int totalDuration = 0;
        int totalDistance = 0;
        for (int i = 0; i < sequence.size() - 1; i += 1) {
            TravelEdge edge = directEdges.get(key(sequence.get(i), sequence.get(i + 1)));
            RouteLeg leg = new RouteLeg();
            leg.setFromTitle(nodes.get(sequence.get(i)).getTitle());
            leg.setToTitle(nodes.get(sequence.get(i + 1)).getTitle());
            leg.setDurationSec(edge.durationSec);
            leg.setMinutes((int) Math.round(edge.durationSec / 60.0));
            leg.setDistanceMeters(edge.distanceMeters);
            leg.setPoints(edge.points);
            totalDuration += edge.durationSec;
            totalDistance += edge.distanceMeters;
            legs.add(leg);
        }
        result.setLegs(legs);
        result.setTotalDurationSec(totalDuration);
        result.setTotalDistanceMeters(totalDistance);
        result.setTotalMinutes((int) Math.round(totalDuration / 60.0));
        return result;
    }

    private List<RouteStop> buildNodes(Plan plan) {
        List<RouteStop> nodes = new ArrayList<>();
        Lodging lodging = plan.getLodging();
        RouteStop home = new RouteStop();
        home.setTitle((lodging.getPlaceName() == null || lodging.getPlaceName().isBlank()) ? "숙소" : lodging.getPlaceName());
        home.setLat(lodging.getLat());
        home.setLng(lodging.getLng());
        home.setLodging(true);
        nodes.add(home);
        for (PlanItem item : plan.getItems()) {
            RouteStop stop = new RouteStop();
            stop.setTitle(item.getTitle());
            stop.setLat(item.getLat());
            stop.setLng(item.getLng());
            stop.setLodging(false);
            nodes.add(stop);
        }
        return nodes;
    }

    private double[][] buildShortestMatrix(double[][] weights) {
        double[][] shortest = new double[weights.length][weights.length];
        for (int src = 0; src < weights.length; src += 1) {
            shortest[src] = dijkstra(weights, src);
        }
        return shortest;
    }

    private double[] dijkstra(double[][] weights, int source) {
        int n = weights.length;
        double[] dist = new double[n];
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i += 1) dist[i] = Double.MAX_VALUE;
        dist[source] = 0;
        for (int count = 0; count < n; count += 1) {
            int u = -1;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < n; i += 1) {
                if (!visited[i] && dist[i] < min) { min = dist[i]; u = i; }
            }
            if (u < 0) break;
            visited[u] = true;
            for (int v = 0; v < n; v += 1) {
                if (!visited[v] && dist[u] + weights[u][v] < dist[v]) {
                    dist[v] = dist[u] + weights[u][v];
                }
            }
        }
        return dist;
    }

    private void permute(List<Integer> order, int index, List<RouteStop> nodes, double[][] shortestMatrix, PermutationResult best) {
        if (index == order.size()) {
            double total = 0;
            int previous = 0;
            for (Integer next : order) { total += shortestMatrix[previous][next]; previous = next; }
            total += shortestMatrix[previous][0];
            if (total < best.total) { best.total = total; best.order = new ArrayList<>(order); }
            return;
        }
        for (int i = index; i < order.size(); i += 1) {
            swap(order, index, i);
            permute(order, index + 1, nodes, shortestMatrix, best);
            swap(order, index, i);
        }
    }

    private void swap(List<Integer> order, int left, int right) {
        Integer temp = order.get(left);
        order.set(left, order.get(right));
        order.set(right, temp);
    }

    private TravelEdge fetchEdge(RouteStop from, RouteStop to) {
        String url = "https://apis-navi.kakaomobility.com/v1/directions?origin=" + from.getLng() + "," + from.getLat()
                + "&destination=" + to.getLng() + "," + to.getLat()
                + "&priority=RECOMMEND&car_fuel=GASOLINE&car_hipass=false";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "KakaoAK " + appProperties.getKakaoMobilityRestKey())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("카카오 모빌리티 API 호출 실패: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode route = root.path("routes").path(0);
            int resultCode = route.path("result_code").asInt(-1);
            if (resultCode != 0) {
                throw new IllegalStateException(
                        "카카오 모빌리티 경로 조회 실패 (result_code=" + resultCode + "): " + route.path("result_msg").asText());
            }
            JsonNode summary = route.path("summary");
            JsonNode roads = route.path("sections").path(0).path("roads");
            TravelEdge edge = new TravelEdge();
            edge.durationSec = summary.path("duration").asInt();
            edge.distanceMeters = summary.path("distance").asInt();
            List<MapPoint> points = new ArrayList<>();
            if (roads.isArray()) {
                for (JsonNode road : roads) {
                    JsonNode vertexes = road.path("vertexes");
                    for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                        points.add(new MapPoint(vertexes.get(i + 1).asDouble(), vertexes.get(i).asDouble()));
                    }
                }
            }
            edge.points = points;
            return edge;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("경로 추천 정보를 조회하지 못했습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("경로 추천 정보를 조회하지 못했습니다.", e);
        }
    }

    private String key(int from, int to) { return from + "->" + to; }

    private static class TravelEdge {
        private int durationSec;
        private int distanceMeters;
        private List<MapPoint> points = new ArrayList<>();
    }

    private static class PermutationResult {
        private double total;
        private List<Integer> order;
        private PermutationResult(double total, List<Integer> order) { this.total = total; this.order = order; }
    }
}
