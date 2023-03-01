package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import com.google.gson.Gson;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     */
    public void echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        msg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + msg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Note getNote(String title) {
        title = title.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();
        Gson gson = new Gson();
        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("GET NOTE", gson.fromJson(body, Note.class).content);
            return gson.fromJson(body, Note.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void putNote(Note note) {
        Gson gson = new Gson();
        String noteJSON = gson.toJson(Map.of("content", note.content, "updated_at", note.updatedAt));
        RequestBody reqBody = RequestBody.create(noteJSON, JSON);
        Log.i("PUT NOTE", reqBody.toString());
        Thread putThread = new Thread(() -> {
            String title = note.title.replace(" ", "%20");
            var request = new Request.Builder()
                    .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                    .method("PUT", reqBody)
                    .build();
            try (var response = client.newCall(request).execute()) {
                assert response.body() != null;
                var body = response.body().string();
                Log.i("PUT NOTE", body);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        putThread.start();
    }
}
