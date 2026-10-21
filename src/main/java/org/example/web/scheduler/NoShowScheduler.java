package org.example.web.scheduler;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.service.RentalService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class NoShowScheduler implements ServletContextListener {

    private static final long CHECK_INTERVAL_MINUTES = 1;

    private final RentalService rentalService = new RentalService(
            new CarDao(),
            new RentalDao(),
            new MemberDao(),
            new ParkingLotDao()
    );

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "no-show-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleWithFixedDelay(
                () -> markExpiredReservations(event),
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler == null) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void markExpiredReservations(ServletContextEvent event) {
        try {
            int changedCount = rentalService.markExpiredReservationsAsNoShow();
            if (changedCount > 0) {
                event.getServletContext().log("노쇼 예약 " + changedCount + "건을 처리했습니다.");
            }
        } catch (RuntimeException e) {
            event.getServletContext().log("노쇼 예약 처리에 실패했습니다.", e);
        }
    }
}
