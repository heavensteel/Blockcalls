package com.floventy.blockcalls.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.floventy.blockcalls.data.AppDatabase;
import com.floventy.blockcalls.data.BlockedNumber;
import com.floventy.blockcalls.data.BlockedNumberDao;
import com.floventy.blockcalls.utils.PatternMatcher;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for MainActivity.
 * Manages blocked numbers list and database operations.
 */
public class MainViewModel extends AndroidViewModel {
    
    private final BlockedNumberDao blockedNumberDao;
    private final LiveData<List<BlockedNumber>> allBlockedNumbers;
    private final ExecutorService executorService;
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        blockedNumberDao = database.blockedNumberDao();
        allBlockedNumbers = blockedNumberDao.getAllBlockedNumbers();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<BlockedNumber>> getAllBlockedNumbers() {
        return allBlockedNumbers;
    }
    
    public void addBlockedNumber(String pattern) {
        executorService.execute(() -> {
            boolean isWildcard = PatternMatcher.isWildcardPattern(pattern);
            BlockedNumber blockedNumber = new BlockedNumber(
                pattern,
                isWildcard,
                System.currentTimeMillis()
            );
            blockedNumberDao.insert(blockedNumber);
        });
    }
    
    public void updateBlockedNumber(BlockedNumber blockedNumber) {
        executorService.execute(() -> {
            boolean isWildcard = PatternMatcher.isWildcardPattern(blockedNumber.getPattern());
            blockedNumber.setWildcard(isWildcard);
            blockedNumberDao.update(blockedNumber);
        });
    }
    
    public void deleteBlockedNumber(BlockedNumber blockedNumber) {
        android.util.Log.d("MainViewModel", "Deleting blocked number: " + blockedNumber.getId() + " - " + blockedNumber.getPattern());
        executorService.execute(() -> {
            // content equality verification not needed for deletion by ID
            blockedNumberDao.deleteById(blockedNumber.getId());
        });
    }
    
    public void deleteBlockedNumberById(int id) {
        executorService.execute(() -> {
            blockedNumberDao.deleteById(id);
        });
    }
    
    public void toggleBlockedNumber(BlockedNumber blockedNumber, boolean isEnabled) {
        executorService.execute(() -> {
            blockedNumber.setEnabled(isEnabled);
            blockedNumberDao.update(blockedNumber);
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
