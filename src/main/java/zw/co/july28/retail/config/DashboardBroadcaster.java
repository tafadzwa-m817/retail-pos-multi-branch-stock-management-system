package zw.co.july28.retail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zw.co.july28.retail.service.DashboardService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final DashboardService dashboardService;

    /** Push live dashboard stats to all subscribed clients every 30 seconds */
    @Scheduled(fixedDelay = 30_000)
    public void broadcastDashboard() {
        try {
            var data = dashboardService.getDashboard();
            messagingTemplate.convertAndSend("/topic/dashboard", data);
        } catch (Exception e) {
            log.debug("Dashboard broadcast skipped: {}", e.getMessage());
        }
    }
}
