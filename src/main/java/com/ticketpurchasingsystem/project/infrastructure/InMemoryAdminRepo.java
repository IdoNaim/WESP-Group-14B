package com.ticketpurchasingsystem.project.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Repository;

import com.ticketpurchasingsystem.project.domain.systemAdmin.AdminInfo;
import com.ticketpurchasingsystem.project.domain.systemAdmin.IAdminRepo;

@Repository
public class InMemoryAdminRepo implements IAdminRepo {

    private final ConcurrentHashMap<String, AdminInfo> storage = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void save(AdminInfo adminInfo) {
        lock.writeLock().lock();
        try {
            storage.put(adminInfo.getId(), adminInfo);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public AdminInfo findById(String adminId) {
        lock.readLock().lock();
        try {
            return storage.get(adminId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isAdmin(String adminId) {
        lock.readLock().lock();
        try {
            return storage.containsKey(adminId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<AdminInfo> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(storage.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}
