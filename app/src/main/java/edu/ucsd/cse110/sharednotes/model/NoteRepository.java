package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NoteRepository {
    private final NoteDao dao;

    // ScheduledFuture results in scheduling a task every 3 seconds with
    // the ScheduledExecutorService.
    private ScheduledFuture<?> future;

    private final NoteAPI noteAPI;


    public NoteRepository(NoteDao dao) {
        this.dao = dao;
        noteAPI = NoteAPI.provide(); //Might have to move?

    }

    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     *
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        var note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = (theirNote) -> {
            var ourNote = note.getValue();
            if (theirNote == null) { return; } // do nothing
            if (ourNote == null || ourNote.version < theirNote.version) {
                upsertLocal(theirNote);
            }

            try {
                Log.i("SYNC", note.getValue().content + note.getValue().version); } catch (Exception e) {};
        };


        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();
    }

    public void upsertLocal(Note note) {
        note.version = note.version + 1;
        if (note.title != null)
            dao.upsert(note);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // TODO: Implement getRemote!
        // TODO: Set up polling background thread (MutableLiveData?)
        // TODO: Refer to TimerService from https://github.com/DylanLukes/CSE-110-WI23-Demo5-V2.

        //Cancel previous poller if it exists.
        if(this.future != null && !this.future.isCancelled()) {
            future.cancel(true);
        }

        MutableLiveData<Note> curNote = new MutableLiveData<Note>();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        this.future = executor.scheduleAtFixedRate(() -> {
            curNote.postValue(noteAPI.getNote(title)); //Changing

            try{ Log.i("REPO", curNote.getValue().content + curNote.getValue().version); }
           catch(Exception e) {}
           }, 0, 3, TimeUnit.SECONDS);


        // Start by fetching the note from the server _once_ and feeding it into MutableLiveData.
        // Then, set up a background thread that will poll the server every 3 seconds.

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.
        // You don't need to worry about killing background threads.

        return curNote;

    }

    public void upsertRemote(Note note) {
        // TODO: Implement upsertRemote!
       // LiveData<Note> curNote = this.getRemote(note.title);
       // if (curNote.getValue().toString().equals("Note not found.")) {
        if(note != null) {
//            if(note.updatedAt == 0){
//                note.updatedAt = System.currentTimeMillis()/1000; // Added to add timestamp to last update.
//            }
            noteAPI.putNote(note);
        }
        //} else
    }
}
