package com.capstone.backend.shop.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanupBefore() {
        cleanup();
    }

    @AfterEach
    void cleanupAfter() {
        cleanup();
    }

    @Test
    void shopItemsReturnBalanceOwnershipAndCharacterEquipState() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopListUser");
        Long ownedItemId = seedItem("skin", "카이렌", 100, true);
        seedItem("skin", "엘리오라", 200, true);
        seedWallet(testUser.userId(), 150);
        seedUserItem(testUser.userId(), ownedItemId, 2);
        jdbcTemplate.update("update users set profile_image_url = '/media/assets/test_item.png' where id = ?", testUser.userId());

        mockMvc.perform(get("/api/shop/items")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceCurrency").value(150))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(ownedItemId))
                .andExpect(jsonPath("$.data.items[0].itemType").value("skin"))
                .andExpect(jsonPath("$.data.items[0].itemTypeLabel").value("캐릭터 스킨"))
                .andExpect(jsonPath("$.data.items[0].category").value("character"))
                .andExpect(jsonPath("$.data.items[0].categoryLabel").value("캐릭터"))
                .andExpect(jsonPath("$.data.items[0].owned").value(true))
                .andExpect(jsonPath("$.data.items[0].ownedQuantity").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].purchasable").value(true))
                .andExpect(jsonPath("$.data.items[0].equipped").value(true))
                .andExpect(jsonPath("$.data.items[0].special").value(false))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("http://localhost:8000/media/assets/test_item.png"))
                .andExpect(jsonPath("$.data.items[1].owned").value(false))
                .andExpect(jsonPath("$.data.items[1].ownedQuantity").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].purchasable").value(false));
    }

    @Test
    void shopItemsSupportTypeFilter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopFilterUser");
        seedItem("skin", "카이렌", 100, true);
        seedItem("profile", "엘리오라", 80, true);
        seedWallet(testUser.userId(), 200);

        mockMvc.perform(get("/api/shop/items")
                        .queryParam("type", "profile")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].itemType").value("profile"))
                .andExpect(jsonPath("$.data.items[0].itemTypeLabel").value("프로필"))
                .andExpect(jsonPath("$.data.items[0].category").value("character"))
                .andExpect(jsonPath("$.data.items[0].categoryLabel").value("캐릭터"))
                .andExpect(jsonPath("$.data.items[0].name").value("엘리오라"));
    }

    @Test
    void shopItemsSupportMobileCategoryFilter() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopMobileCategoryUser");
        seedItem("skin", "카이렌", 0, true);
        seedItem("skin", "엘리오라", 80, true);
        Long passItemId = seedItem("ticket", "퀘스트 스킵권", 150, true);
        seedWallet(testUser.userId(), 200);
        seedUserItem(testUser.userId(), passItemId, 3);

        mockMvc.perform(get("/api/shop/items")
                        .queryParam("type", "character")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].category").value("character"))
                .andExpect(jsonPath("$.data.items[1].category").value("character"));

        mockMvc.perform(get("/api/shop/items")
                        .queryParam("type", "pass")
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].category").value("pass"))
                .andExpect(jsonPath("$.data.items[0].categoryLabel").value("패스"))
                .andExpect(jsonPath("$.data.items[0].owned").value(true))
                .andExpect(jsonPath("$.data.items[0].ownedQuantity").value(3));
    }

    @Test
    void purchaseItemDebitsWalletAddsInventoryAndLedger() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopPurchaseUser");
        Long itemId = seedItem("ticket", "경험치 2배권", 40, true);
        seedWallet(testUser.userId(), 150);
        seedUserItem(testUser.userId(), itemId, 1);

        mockMvc.perform(post("/api/shop/items/{itemId}/purchase", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("아이템을 구매했습니다."))
                .andExpect(jsonPath("$.data.item.itemId").value(itemId))
                .andExpect(jsonPath("$.data.item.itemTypeLabel").value("이용권"))
                .andExpect(jsonPath("$.data.item.quantity").value(3))
                .andExpect(jsonPath("$.data.balanceCurrency").value(70))
                .andExpect(jsonPath("$.data.transaction.txType").value("purchase"))
                .andExpect(jsonPath("$.data.transaction.amount").value(-80));

        Integer balance = jdbcTemplate.queryForObject(
                "select balance_currency from wallets where user_id = ?",
                Integer.class,
                testUser.userId()
        );
        Integer quantity = jdbcTemplate.queryForObject(
                "select quantity from user_items where user_id = ? and item_id = ?",
                Integer.class,
                testUser.userId(),
                itemId
        );
        Integer purchaseAmount = jdbcTemplate.queryForObject(
                "select amount from wallet_transactions where user_id = ? and item_id = ? and tx_type = 'purchase'",
                Integer.class,
                testUser.userId(),
                itemId
        );
        org.assertj.core.api.Assertions.assertThat(balance).isEqualTo(70);
        org.assertj.core.api.Assertions.assertThat(quantity).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(purchaseAmount).isEqualTo(-80);
    }

    @Test
    void purchaseItemRejectsInsufficientBalance() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopInsufficientUser");
        Long itemId = seedItem("skin", "카이렌", 100, true);
        seedWallet(testUser.userId(), 50);

        mockMvc.perform(post("/api/shop/items/{itemId}/purchase", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken())
                        .contentType("application/json")
                        .content("""
                                {
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

        Integer balance = jdbcTemplate.queryForObject(
                "select balance_currency from wallets where user_id = ?",
                Integer.class,
                testUser.userId()
        );
        Integer transactionCount = jdbcTemplate.queryForObject(
                "select count(*) from wallet_transactions where user_id = ?",
                Integer.class,
                testUser.userId()
        );
        org.assertj.core.api.Assertions.assertThat(balance).isEqualTo(50);
        org.assertj.core.api.Assertions.assertThat(transactionCount).isZero();
    }

    @Test
    void equipOwnedImageItemUpdatesProfileImageUrl() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopEquipUser");
        Long itemId = seedItem("profile", "카이렌", 80, true);
        seedUserItem(testUser.userId(), itemId, 1);

        mockMvc.perform(post("/api/shop/items/{itemId}/equip", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("아이템을 장착했습니다."))
                .andExpect(jsonPath("$.data.item.itemId").value(itemId))
                .andExpect(jsonPath("$.data.item.itemType").value("profile"))
                .andExpect(jsonPath("$.data.item.itemTypeLabel").value("프로필"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("/media/assets/test_item.png"));

        String profileImageUrl = jdbcTemplate.queryForObject(
                "select profile_image_url from users where id = ?",
                String.class,
                testUser.userId()
        );
        org.assertj.core.api.Assertions.assertThat(profileImageUrl).isEqualTo("/media/assets/test_item.png");
    }

    @Test
    void equipItemRequiresOwnership() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopEquipOwnershipUser");
        Long itemId = seedItem("profile", "카이렌", 80, true);

        mockMvc.perform(post("/api/shop/items/{itemId}/equip", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_OWNED"));
    }

    @Test
    void equipItemRejectsNonImageEquippableItem() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopEquipConsumableUser");
        Long itemId = seedItem("ticket", "경험치 2배권", 30, true);
        seedUserItem(testUser.userId(), itemId, 1);

        mockMvc.perform(post("/api/shop/items/{itemId}/equip", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_EQUIPPABLE"));
    }

    @Test
    void useExpBoostPassConsumesInventoryAndActivatesEffect() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopUseExpBoostUser");
        Long itemId = seedItem("ticket", "경험치 2배권", 200, true);
        seedUserItem(testUser.userId(), itemId, 2);

        mockMvc.perform(post("/api/shop/items/{itemId}/use", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("아이템을 사용했습니다."))
                .andExpect(jsonPath("$.data.item.itemId").value(itemId))
                .andExpect(jsonPath("$.data.item.quantity").value(1))
                .andExpect(jsonPath("$.data.effectType").value("EXP_BOOST"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.expiresAt").exists());

        Integer quantity = jdbcTemplate.queryForObject(
                "select quantity from user_items where user_id = ? and item_id = ?",
                Integer.class,
                testUser.userId(),
                itemId
        );
        Integer effectCount = jdbcTemplate.queryForObject(
                "select count(*) from user_item_effects where user_id = ? and item_id = ? and effect_type = 'EXP_BOOST' and status = 'ACTIVE'",
                Integer.class,
                testUser.userId(),
                itemId
        );
        org.assertj.core.api.Assertions.assertThat(quantity).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(effectCount).isEqualTo(1);
    }

    @Test
    void usePassRequiresOwnership() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        TestUser testUser = testUser("shopUseOwnershipUser");
        Long itemId = seedItem("ticket", "경험치 2배권", 200, true);

        mockMvc.perform(post("/api/shop/items/{itemId}/use", itemId)
                        .header("Authorization", "Bearer " + testUser.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_OWNED"));
    }

    private void cleanup() {
        jdbcTemplate.update("delete from battle_match_queue");

        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_item_effects");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from user_items");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from items");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from health_daily_summaries");

        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    private TestUser testUser(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return new TestUser(user.getId(), jwtTokenService.issueTokenPair(user).accessToken());
    }

    private Long seedItem(String itemType, String name, int priceCurrency, boolean sellable) {
        return insertAndReturnId("""
                insert into items (
                    item_type,
                    name,
                    description,
                    price_currency,
                    is_sellable,
                    image_url,
                    metadata,
                    is_active,
                    created_at
                )
                values (?, ?, '테스트 아이템입니다.', ?, ?, '/media/assets/test_item.png', JSON '{}', true, CURRENT_TIMESTAMP)
                """, itemType, name, priceCurrency, sellable);
    }

    private void seedWallet(Long userId, int balanceCurrency) {
        jdbcTemplate.update("""
                insert into wallets (user_id, balance_currency, updated_at)
                values (?, ?, CURRENT_TIMESTAMP)
                """, userId, balanceCurrency);
    }

    private void seedUserItem(Long userId, Long itemId, int quantity) {
        jdbcTemplate.update("""
                insert into user_items (user_id, item_id, quantity, updated_at)
                values (?, ?, ?, CURRENT_TIMESTAMP)
                """, userId, itemId, quantity);
    }

    private Long insertAndReturnId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement;
        };
        jdbcTemplate.update(creator, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private record TestUser(Long userId, String accessToken) {
    }
}
