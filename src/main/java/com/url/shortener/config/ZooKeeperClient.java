package com.url.shortener.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Getter
public class ZooKeeperClient implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "172.19.0.2:2181";
    public static final String COUNTER_NODE = "/counter";
    private static final int SESSION_TIMEOUT = 3000;
    public static final String INITIAL_COUNTER = "100000000000";
    private volatile boolean connected = false;
    private ZooKeeper zooKeeper;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);

    public void connect() throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        this.connected = true;
        connectedSignal.await();
        log.info("Connection created");
        System.out.println("Connected");
    }

    public Boolean existsNode(String node) {
        try {
            log.info("Checking if node exists");
            return zooKeeper.exists(node, false) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("Error checking if node exists", e);
            return null;
        }
    }

    public void createNode(String node, String value) throws Exception {
        try {
            zooKeeper.create(node, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            log.error("Error creating node {} with value {}: {}", node, value, e.getMessage());
            throw new Exception(e);
        }
    }

    public synchronized long getNextId() throws Exception {
        byte[] data = zooKeeper.getData(COUNTER_NODE, false, null);
        long currentValue = Long.parseLong(new String(data));
        long nextValue = currentValue + 1;
        zooKeeper.setData(COUNTER_NODE, String.valueOf(nextValue).getBytes(), -1);
        return currentValue;
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
            connectedSignal.countDown();
        } else if (event.getState() == Event.KeeperState.Disconnected) {
            this.connected = false;
            log.info("Zookeeper client is disconnected");
        } else if (event.getState() == Event.KeeperState.Expired) {
            try {
                close();
                this.connected = false;
            } catch (InterruptedException e) {
                log.error("Error closing ZookeeperConnection: {}", e.getLocalizedMessage());
            }
        }
    }
}
