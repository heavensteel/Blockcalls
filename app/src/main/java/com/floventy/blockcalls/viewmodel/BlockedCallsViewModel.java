package com.floventy.blockcalls.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.floventy.blockcalls.data.AppDatabase;
import com.floventy.blockcalls.data.BlockedCall;
import com.floventy.blockcalls.data.BlockedCallDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for BlockedCallsActivity.
 * Manages blocked calls log and database operations.
 */
public class BlockedCallsViewModel extends AndroidViewModel {

    private final BlockedCallDao blockedCallDao;
    private final LiveData<List<BlockedCall>> allBlockedCalls;
    private final ExecutorService executorService;

    public BlockedCallsViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        blockedCallDao = database.blockedCallDao();
        allBlockedCalls = blockedCallDao.getAllBlockedCalls();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<BlockedCall>> getAllBlockedCalls() {
        return allBlockedCalls;
    }

    public void clearAllBlockedCalls() {
        executorService.execute(() -> {
            blockedCallDao.deleteAll();
        });
    }

    /**
     * Add a test blocked call entry (for testing/debugging purposes).
     */
    public void addTestBlockedCall(String phoneNumber, String matchedPattern) {
        executorService.execute(() -> {
            BlockedCall testCall = new BlockedCall(
                    phoneNumber,
                    System.currentTimeMillis(),
                    matchedPattern);
            blockedCallDao.insert(testCall);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
