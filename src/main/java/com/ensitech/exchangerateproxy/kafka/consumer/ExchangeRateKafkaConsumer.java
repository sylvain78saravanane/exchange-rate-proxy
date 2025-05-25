package com.ensitech.exchangerateproxy.kafka.consumer;

import com.ensitech.exchangerateproxy.model.ExchangeRateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateKafkaConsumer {

    /**
     * Consommateur Kafka pour traiter les messages de taux de change
     */
    @KafkaListener(topics = "${exchange-rate.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload ExchangeRateData exchangeRateData,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment acknowledgment) {

        try {
            log.info("Received exchange rate data from Kafka - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    topic, partition, offset, key);
            log.debug("Exchange rate data: Base currency: {}, Rates count: {}, Timestamp: {}",
                    exchangeRateData.getBaseCurrency(),
                    exchangeRateData.getRates() != null ? exchangeRateData.getRates().size() : 0,
                    exchangeRateData.getTimestamp());

            processExchangeRateData(exchangeRateData);

            // Acquittement manuel du message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing exchange rate data: {}", e.getMessage(), e);
        }
    }

    /**
     * Traite les données de taux de change reçues
     */
    private void processExchangeRateData(ExchangeRateData exchangeRateData) {

        log.info("Processing exchange rate data for currency: {}", exchangeRateData.getBaseCurrency());

        // Vérification de la validité des données
        if (exchangeRateData.getRates() == null || exchangeRateData.getRates().isEmpty()) {
            log.warn("Received exchange rate data with no rates");
            return;
        }

        // Usage de stream API
        exchangeRateData.getRates().entrySet().stream()
                .filter(entry -> isMainCurrency(entry.getKey()))
                .forEach(entry ->
                        log.debug("Rate for {}: {}", entry.getKey(), entry.getValue())
                );

    }

    /**
     * Vérifie si une devise fait partie des principales devises surveillées
     */
    private boolean isMainCurrency(String currency) {
        return currency.matches("EUR|GBP|JPY|CHF|CAD|AUD|CNY");
    }
}
