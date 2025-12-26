package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import model.Karyawan;

public class KaryawanApiClient implements WebSocket.Listener {
    private static final String WS_URL = "ws://localhost:3000";
    private WebSocket webSocket;
    private final HttpClient httpClient;
    private final Gson gson;
    private CompletableFuture<String> responseFuture;
    private final ReentrantLock lock = new ReentrantLock();

    public KaryawanApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        
        connectWebSocket();
    }

    private void connectWebSocket() {
        try {
            CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(WS_URL), this);
            
            this.webSocket = wsFuture.get(10, TimeUnit.SECONDS);
            System.out.println("WebSocket connected");
        } catch (Exception e) {
            System.err.println("Failed to connect WebSocket: " + e.getMessage());
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WebSocket opened");
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();
        
        if (!message.contains("GET_ALL_RESPONSE") || message.length() < 100) {
            System.out.println("Received: " + 
                (message.length() > 100 ? message.substring(0, 100) + "..." : message));
        } else {
            System.out.println("Received employee data (" + message.length() + " chars)");
        }
        
        try {
            lock.lock();
            if (responseFuture != null && !responseFuture.isDone()) {
                responseFuture.complete(message);
            }
        } finally {
            lock.unlock();
        }
        
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
        this.webSocket = null;
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
        if (responseFuture != null && !responseFuture.isDone()) {
            responseFuture.completeExceptionally(error);
        }
    }

    private String sendAndWait(String message) throws Exception {
        if (webSocket == null) {
            throw new RuntimeException("WebSocket not connected");
        }

        responseFuture = new CompletableFuture<>();
        
        String action = extractAction(message);
        System.out.println("Sending: " + action);
        
        webSocket.sendText(message, true);
        
        try {
            return responseFuture.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            responseFuture.cancel(true);
            throw new RuntimeException("Timeout waiting for WebSocket response", e);
        }
    }

    private String extractAction(String json) {
        try {
            int actionIndex = json.indexOf("\"action\":");
            if (actionIndex != -1) {
                int start = actionIndex + 9;
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    return json.substring(start, end);
                }
            }
            return "UNKNOWN_ACTION";
        } catch (Exception e) {
            return "ERROR_EXTRACTING";
        }
    }

    public List<Karyawan> findAll() throws Exception {
        String request = gson.toJson(new WebSocketMessage("GET_ALL_KARYAWAN", null));
        String response = sendAndWait(request);
        
        WebSocketResponse wsResponse = gson.fromJson(response, WebSocketResponse.class);
        if (wsResponse.type.equals("GET_ALL_RESPONSE") && wsResponse.success) {
            String dataJson = gson.toJson(wsResponse.data);
            return gson.fromJson(dataJson, new TypeToken<List<Karyawan>>() {}.getType());
        } else {
            throw new RuntimeException("Failed to get karyawan list: " + 
                    (wsResponse.message != null ? wsResponse.message : "Unknown error"));
        }
    }

    public Karyawan create(Karyawan k) throws Exception {
        CreateKaryawanPayload payload = new CreateKaryawanPayload(
            k.getEmployeeId(),
            k.getName(),
            k.getDepartment(),
            k.getPosition(),
            k.getSalary(),
            k.getHireDate() != null ? k.getHireDate().toString() : null,
            k.getEmail(),
            k.getPhone()
        );
        
        String request = gson.toJson(new WebSocketMessage("CREATE_KARYAWAN", payload));
        String response = sendAndWait(request);
        
        WebSocketResponse wsResponse = gson.fromJson(response, WebSocketResponse.class);
        if (wsResponse.type.equals("CREATE_RESPONSE") && wsResponse.success) {
            String dataJson = gson.toJson(wsResponse.data);
            return gson.fromJson(dataJson, Karyawan.class);
        } else {
            throw new RuntimeException("Failed to create karyawan: " + 
                    (wsResponse.message != null ? wsResponse.message : "Unknown error"));
        }
    }

    public Karyawan update(Karyawan k) throws Exception {
        UpdateKaryawanPayload payload = new UpdateKaryawanPayload(
            k.getId(),
            k.getEmployeeId(),
            k.getName(),
            k.getDepartment(),
            k.getPosition(),
            k.getSalary(),
            k.getHireDate() != null ? k.getHireDate().toString() : null,
            k.getEmail(),
            k.getPhone()
        );
        
        String request = gson.toJson(new WebSocketMessage("UPDATE_KARYAWAN", payload));
        String response = sendAndWait(request);
        
        WebSocketResponse wsResponse = gson.fromJson(response, WebSocketResponse.class);
        if (wsResponse.type.equals("UPDATE_RESPONSE") && wsResponse.success) {
            return k;
        } else {
            throw new RuntimeException("Failed to update karyawan: " + 
                    (wsResponse.message != null ? wsResponse.message : "Unknown error"));
        }
    }

    public void delete(int id) throws Exception {
        DeleteKaryawanPayload payload = new DeleteKaryawanPayload(id);
        
        String request = gson.toJson(new WebSocketMessage("DELETE_KARYAWAN", payload));
        String response = sendAndWait(request);
        
        WebSocketResponse wsResponse = gson.fromJson(response, WebSocketResponse.class);
        if (!wsResponse.type.equals("DELETE_RESPONSE") || !wsResponse.success) {
            throw new RuntimeException("Failed to delete karyawan: " + 
                    (wsResponse.message != null ? wsResponse.message : "Unknown error"));
        }
    }
    
    public Karyawan findById(int id) throws Exception {
        List<Karyawan> allKaryawan = findAll();
        return allKaryawan.stream()
                .filter(k -> k.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Karyawan not found with id: " + id));
    }

    private static class WebSocketMessage {
        String action;
        Object payload;
        
        WebSocketMessage(String action, Object payload) {
            this.action = action;
            this.payload = payload;
        }
    }

    private static class WebSocketResponse {
        String type;
        boolean success;
        Object data;
        String message;
    }

    private static class CreateKaryawanPayload {
        String employeeId;
        String name;
        String department;
        String position;
        double salary;
        String hireDate;
        String email;
        String phone;
        
        CreateKaryawanPayload(String employeeId, String name, String department, 
                             String position, double salary, String hireDate, 
                             String email, String phone) {
            this.employeeId = employeeId;
            this.name = name;
            this.department = department;
            this.position = position;
            this.salary = salary;
            this.hireDate = hireDate;
            this.email = email;
            this.phone = phone;
        }
    }

    private static class UpdateKaryawanPayload {
        int id;
        String employeeId;
        String name;
        String department;
        String position;
        double salary;
        String hireDate;
        String email;
        String phone;
        
        UpdateKaryawanPayload(int id, String employeeId, String name, String department, 
                             String position, double salary, String hireDate, 
                             String email, String phone) {
            this.id = id;
            this.employeeId = employeeId;
            this.name = name;
            this.department = department;
            this.position = position;
            this.salary = salary;
            this.hireDate = hireDate;
            this.email = email;
            this.phone = phone;
        }
    }

    private static class DeleteKaryawanPayload {
        int id;
        
        DeleteKaryawanPayload(int id) {
            this.id = id;
        }
    }
}