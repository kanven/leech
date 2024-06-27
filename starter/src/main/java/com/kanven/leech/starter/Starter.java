package com.kanven.leech.starter;

import com.kanven.leech.fetcher.Fetcher;
import com.kanven.leech.fetcher.FileEntry;
import com.kanven.leech.sched.ScheduleEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Starter {

    public static void main(String[] args) {
        ScheduleEngine<FileEntry> scheduler = new ScheduleEngine<>();
        scheduler.start();
        Fetcher fetcher = new Fetcher();
        fetcher.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    fetcher.close();
                    scheduler.close();
                } catch (Exception e) {
                    log.error("the server stop has an error", e);
                }
            }
        });
    }

}
