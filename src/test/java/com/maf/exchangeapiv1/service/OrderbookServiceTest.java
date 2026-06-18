package com.maf.exchangeapiv1.service;

import com.maf.exchangeapiv1.cache.OrderBookCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class OrderbookServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    private OrderBookCache orderBookCache;
    private     RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getMappedPort(6379));

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        orderBookCache = new OrderBookCache(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        // To clean redis after all test operations
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }


    @Test
    @DisplayName("UpdatePrice")
    void updatePrice_shouldSavePriceToRedis() {
        String pair = "BTC-USD";
        BigDecimal price = new BigDecimal("50000.00");

        orderBookCache.updatePrice(pair, price);
        BigDecimal result = orderBookCache.getSelectedPriceTicker(pair);

        assertThat(result).isEqualTo(price);
    }

    @Test
    @DisplayName("GetSelectedPriceTicker - Return 0")
    void getSelectedPriceTicker_forNonExistentPair_shouldReturnZero() {
        BigDecimal result = orderBookCache.getSelectedPriceTicker("NONEXISTENT-USD");
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GetAllPricesTicker - Return All")
    void getAllPricesTicker_shouldReturnAllPrices() {
        orderBookCache.updatePrice("BTC-USD", new BigDecimal("50000"));
        orderBookCache.updatePrice("ETH-USD", new BigDecimal("3000"));

        Map<String, BigDecimal> allPrices = orderBookCache.getAllPricesTicker();

        assertThat(allPrices).hasSize(2);
        assertThat(allPrices.get("BTC-USD")).isEqualTo(new BigDecimal("50000"));
        assertThat(allPrices.get("ETH-USD")).isEqualTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("UpdateOrderBook")
    void updateOrderBook_shouldAddAskPrices() {
        String pair = "BTC-USD";

        orderBookCache.updateOrderBook(pair, "sell", "30000", "1.5");
        orderBookCache.updateOrderBook(pair, "sell", "30050", "0.8");
        orderBookCache.updateOrderBook(pair, "sell", "30100", "2.0");

        BigDecimal bestAsk = orderBookCache.getBestAsk(pair);
        assertThat(bestAsk).isEqualTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("RemovePriceLevel ")
    void removePriceLevel_shouldRemovePriceLevel() {
        String pair = "BTC-USD";
        orderBookCache.updateOrderBook(pair, "sell", "30000", "1.5");
        assertThat(orderBookCache.getBestAsk(pair)).isEqualTo(new BigDecimal("30000"));

        orderBookCache.removePriceLevel(pair, "sell", "30000");
        assertThat(orderBookCache.getBestAsk(pair)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GetBestAsk - Return 0")
    void getBestAsk_whenOrderBookEmpty_shouldReturnZero() {
        BigDecimal bestAsk = orderBookCache.getBestAsk("BTC-USD");
        assertThat(bestAsk).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GetTotalCostForAmount")
    void getTotalCostForAmount_shouldReturnCorrectTotalCost() {
        String pair = "BTC-USD";
        orderBookCache.updateOrderBook(pair, "sell", "30000", "0.5");
        orderBookCache.updateOrderBook(pair, "sell", "30050", "0.3");
        orderBookCache.updateOrderBook(pair, "sell", "30100", "0.2");

        BigDecimal totalCost = orderBookCache.getTotalCostForAmount(pair, BigDecimal.ONE, true);
        BigDecimal expected = new BigDecimal("30035.00000000");

        assertThat(totalCost).isEqualTo(expected);
    }

    @Test
    @DisplayName("GetAveragePriceWithVolume")
    void getAveragePriceWithVolume_shouldReturnCorrectAveragePrice() {
        String pair = "BTC-USD";
        orderBookCache.updateOrderBook(pair, "sell", "30000", "0.5");
        orderBookCache.updateOrderBook(pair, "sell", "30050", "0.3");
        orderBookCache.updateOrderBook(pair, "sell", "30100", "0.2");

        BigDecimal avgPrice = orderBookCache.getAveragePriceWithVolume(pair, BigDecimal.ONE, true);
        BigDecimal expected = new BigDecimal("30035.00000000");

        assertThat(avgPrice).isEqualTo(expected);
    }



}