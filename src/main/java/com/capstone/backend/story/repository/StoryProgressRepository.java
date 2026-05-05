package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.StoryProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryProgressRepository extends JpaRepository<StoryProgress, Integer> {

    Optional<StoryProgress> findTopByScenario_IdAndUserKeyOrderByUpdatedAtDesc(Integer scenarioId, String userKey);

    @Query("""
            select progress
            from StoryProgress progress
            join fetch progress.scenario scenario
            where progress.userKey = :userKey
              and scenario.active = true
            order by progress.updatedAt desc, progress.id desc
            """)
    List<StoryProgress> findActiveChatsByUserKey(@Param("userKey") String userKey, Pageable pageable);

    @Query("""
            select progress
            from StoryProgress progress
            join fetch progress.scenario scenario
            where progress.scenario.id = :scenarioId
              and progress.userKey = :userKey
              and scenario.active = true
            order by progress.updatedAt desc, progress.id desc
            """)
    List<StoryProgress> findActiveChatByScenarioIdAndUserKey(@Param("scenarioId") Integer scenarioId,
                                                             @Param("userKey") String userKey,
                                                             Pageable pageable);

    @Query("""
            select count(progress)
            from StoryProgress progress
            where progress.userKey = :userKey
              and progress.scenario.active = true
            """)
    long countActiveChatsByUserKey(@Param("userKey") String userKey);

    @Query("""
            select count(distinct progress.userKey)
            from StoryProgress progress
            where progress.scenario.id = :scenarioId
              and progress.status = :status
            """)
    long countDistinctUserKeyByScenarioIdAndStatus(@Param("scenarioId") Integer scenarioId,
                                                   @Param("status") String status);

    @Query("""
            select progress.scenario.id as scenarioId,
                   count(distinct progress.userKey) as score
            from StoryProgress progress
            where progress.scenario.active = true
              and progress.status = :status
            group by progress.scenario.id
            order by count(distinct progress.userKey) desc, progress.scenario.id asc
            """)
    List<WorldRankingRow> findWorldRankingRows(@Param("status") String status, Pageable pageable);

    @Query(
            value = """
                    select
                        s.id as "scenarioId",
                        coalesce(count(distinct sp.user_key), 0) as "score"
                    from scenarios s
                    left join story_progress sp
                        on sp.scenario_id = s.id
                       and sp.status = :status
                    where s.is_active = true
                      and (
                        :genre is null
                        or exists (
                            select 1
                            from scenario_genres sg
                            where sg.scenario_id = s.id
                              and lower(sg.genre_name) = lower(:genre)
                        )
                        or lower(coalesce(s.genre, '')) like concat('%', lower(:genre), '%')
                      )
                      and (
                        :keyword is null
                        or lower(coalesce(s.title, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(s.summary, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(s.genre, '')) like concat('%', lower(:keyword), '%')
                        or exists (
                            select 1
                            from scenario_genres sg
                            where sg.scenario_id = s.id
                              and lower(sg.genre_name) like concat('%', lower(:keyword), '%')
                        )
                        or exists (
                            select 1
                            from character_profiles cp
                            where cp.scenario_id = s.id
                              and (
                                lower(coalesce(cp.name, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.character_title, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.character_type, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.tags, '')) like concat('%', lower(:keyword), '%')
                              )
                        )
                      )
                    group by s.id
                    order by coalesce(count(distinct sp.user_key), 0) desc, s.id asc
                    """,
            countQuery = """
                    select count(*)
                    from scenarios s
                    where s.is_active = true
                      and (
                        :genre is null
                        or exists (
                            select 1
                            from scenario_genres sg
                            where sg.scenario_id = s.id
                              and lower(sg.genre_name) = lower(:genre)
                        )
                        or lower(coalesce(s.genre, '')) like concat('%', lower(:genre), '%')
                      )
                      and (
                        :keyword is null
                        or lower(coalesce(s.title, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(s.summary, '')) like concat('%', lower(:keyword), '%')
                        or lower(coalesce(s.genre, '')) like concat('%', lower(:keyword), '%')
                        or exists (
                            select 1
                            from scenario_genres sg
                            where sg.scenario_id = s.id
                              and lower(sg.genre_name) like concat('%', lower(:keyword), '%')
                        )
                        or exists (
                            select 1
                            from character_profiles cp
                            where cp.scenario_id = s.id
                              and (
                                lower(coalesce(cp.name, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.character_title, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.character_type, '')) like concat('%', lower(:keyword), '%')
                                or lower(coalesce(cp.tags, '')) like concat('%', lower(:keyword), '%')
                              )
                        )
                      )
                    """,
            nativeQuery = true
    )
    Page<WorldRankingRow> findWorldRankingFullRows(@Param("status") String status,
                                                   @Param("genre") String genre,
                                                   @Param("keyword") String keyword,
                                                   Pageable pageable);

    interface WorldRankingRow {
        Integer getScenarioId();

        Long getScore();
    }
}
