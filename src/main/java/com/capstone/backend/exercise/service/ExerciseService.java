package com.capstone.backend.exercise.service;

import com.capstone.backend.exercise.dto.ExerciseCardResponse;
import com.capstone.backend.exercise.dto.ExerciseCategoryListResponse;
import com.capstone.backend.exercise.dto.ExerciseCategoryResponse;
import com.capstone.backend.exercise.dto.ExerciseDetailResponse;
import com.capstone.backend.exercise.dto.ExerciseFavoriteResponse;
import com.capstone.backend.exercise.dto.ExerciseListResponse;
import com.capstone.backend.exercise.dto.ExerciseListResponse.ExerciseGroupResponse;
import com.capstone.backend.exercise.entity.UserFavoriteExercise;
import com.capstone.backend.exercise.repository.ExerciseRepository;
import com.capstone.backend.exercise.repository.UserFavoriteExerciseRepository;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {

    private static final Set<String> ALLOWED_SCOPES = Set.of("all", "favorite", "recent");
    private static final BigDecimal FALLBACK_MALE_WEIGHT_KG = BigDecimal.valueOf(75);
    private static final BigDecimal FALLBACK_FEMALE_WEIGHT_KG = BigDecimal.valueOf(60);
    private static final BigDecimal FALLBACK_UNDISCLOSED_GENDER_WEIGHT_KG = BigDecimal.valueOf(65);
    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(70);

    private final ExerciseRepository exerciseRepository;
    private final UserFavoriteExerciseRepository favoriteExerciseRepository;
    private final UserRepository userRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
                           UserFavoriteExerciseRepository favoriteExerciseRepository,
                           UserRepository userRepository) {
        this.exerciseRepository = exerciseRepository;
        this.favoriteExerciseRepository = favoriteExerciseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ExerciseCategoryListResponse getCategories(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = clampSize(size);
        List<ExerciseCategoryResponse> allCategories = exerciseRepository.findDistinctCategories().stream()
                .map(ExerciseCategoryResponse::from)
                .toList();
        int totalCount = allCategories.size();
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / normalizedSize);
        int fromIndex = Math.min(normalizedPage * normalizedSize, totalCount);
        int toIndex = Math.min(fromIndex + normalizedSize, totalCount);
        List<ExerciseCategoryResponse> pageCategories = allCategories.subList(fromIndex, toIndex);
        boolean first = normalizedPage == 0;
        boolean last = totalPages == 0 || normalizedPage >= totalPages - 1;
        boolean hasNext = !last;
        return new ExerciseCategoryListResponse(
                normalizedPage,
                normalizedSize,
                totalCount,
                totalPages,
                first,
                last,
                hasNext,
                hasNext ? normalizedPage + 1 : null,
                pageCategories
        );
    }

    @Transactional(readOnly = true)
    public ExerciseListResponse getExercises(Long userId,
                                             String scope,
                                             String category,
                                             String level,
                                             String keyword,
                                             int page,
                                             int size) {
        String normalizedScope = normalizeScope(scope);
        User user = findUser(userId);
        BigDecimal effectiveWeightKg = resolveEffectiveWeightKg(user);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("category").ascending().and(Sort.by("name").ascending()));

        List<Long> favoriteExerciseIds = favoriteExerciseRepository.findExerciseIdsByUserId(userId);
        if ("favorite".equals(normalizedScope) && favoriteExerciseIds.isEmpty()) {
            return emptyExerciseListResponse(normalizedScope, category, level, keyword, pageable);
        }
        if ("recent".equals(normalizedScope)) {
            // Recent exercise tracking is not stored yet. Return an empty tab-compatible response for now.
            return emptyExerciseListResponse(normalizedScope, category, level, keyword, pageable);
        }

        Page<Exercise> exercisePage = exerciseRepository.findAll(
                exerciseSpecification(normalizedScope, favoriteExerciseIds, category, level, keyword),
                pageable
        );
        List<Long> pageExerciseIds = exercisePage.getContent().stream().map(Exercise::getId).toList();
        Set<Long> likedIds = pageExerciseIds.isEmpty()
                ? Set.of()
                : favoriteExerciseRepository.findExerciseIdsByUserIdAndExerciseIds(userId, pageExerciseIds).stream()
                .collect(Collectors.toSet());

        List<ExerciseCardResponse> cards = exercisePage.getContent().stream()
                .map(exercise -> ExerciseCardResponse.from(exercise, likedIds.contains(exercise.getId()), effectiveWeightKg))
                .toList();

        return new ExerciseListResponse(
                normalizedScope,
                category,
                level,
                keyword,
                exercisePage.getNumber(),
                exercisePage.getSize(),
                exercisePage.getTotalElements(),
                exercisePage.getTotalPages(),
                exercisePage.isFirst(),
                exercisePage.isLast(),
                exercisePage.hasNext(),
                exercisePage.hasNext() ? exercisePage.getNumber() + 1 : null,
                groupByCategory(cards)
        );
    }

    @Transactional(readOnly = true)
    public ExerciseDetailResponse getExercise(Long userId, Long exerciseId) {
        User user = findUser(userId);
        Exercise exercise = findExercise(exerciseId);
        boolean liked = favoriteExerciseRepository.existsByUser_IdAndExercise_Id(userId, exerciseId);
        return ExerciseDetailResponse.from(exercise, liked, resolveEffectiveWeightKg(user));
    }

    @Transactional(readOnly = true)
    public ExerciseListResponse getFavoriteExercises(Long userId,
                                                     String category,
                                                     String level,
                                                     String keyword,
                                                     int page,
                                                     int size) {
        return getExercises(userId, "favorite", category, level, keyword, page, size);
    }

    @Transactional
    public ExerciseFavoriteResponse updateFavorite(Long userId, Long exerciseId, boolean liked) {
        User user = findUser(userId);
        Exercise exercise = findExercise(exerciseId);

        if (liked) {
            favoriteExerciseRepository.findByUser_IdAndExercise_Id(userId, exerciseId)
                    .orElseGet(() -> favoriteExerciseRepository.save(UserFavoriteExercise.create(user, exercise)));
            return new ExerciseFavoriteResponse(exerciseId, true);
        }

        favoriteExerciseRepository.findByUser_IdAndExercise_Id(userId, exerciseId)
                .ifPresent(favoriteExerciseRepository::delete);
        return new ExerciseFavoriteResponse(exerciseId, false);
    }

    private Exercise findExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND", "운동을 찾을 수 없습니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private BigDecimal resolveEffectiveWeightKg(User user) {
        if (user.getWeightKg() != null) {
            return user.getWeightKg();
        }
        return switch (user.getGender() == null ? "" : user.getGender()) {
            case "male" -> FALLBACK_MALE_WEIGHT_KG;
            case "female" -> FALLBACK_FEMALE_WEIGHT_KG;
            case "prefer_not_to_say" -> FALLBACK_UNDISCLOSED_GENDER_WEIGHT_KG;
            default -> DEFAULT_WEIGHT_KG;
        };
    }

    private String normalizeScope(String scope) {
        String normalizedScope = scope == null || scope.isBlank() ? "all" : scope;
        if (!ALLOWED_SCOPES.contains(normalizedScope)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXERCISE_SCOPE", "운동 목록 범위 값이 올바르지 않습니다.");
        }
        return normalizedScope;
    }

    private Specification<Exercise> exerciseSpecification(String scope,
                                                          List<Long> favoriteExerciseIds,
                                                          String category,
                                                          String level,
                                                          String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if ("favorite".equals(scope)) {
                predicates.add(root.get("id").in(favoriteExerciseIds));
            }
            String normalizedCategory = normalizeCategoryFilter(category);
            if (normalizedCategory != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), normalizedCategory));
            }
            String normalizedLevel = normalizeLevelFilter(level);
            if (normalizedLevel != null) {
                predicates.add(criteriaBuilder.equal(root.get("level"), normalizedLevel));
            }
            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeKeyword));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private ExerciseListResponse emptyExerciseListResponse(String scope,
                                                           String category,
                                                           String level,
                                                           String keyword,
                                                           Pageable pageable) {
        return new ExerciseListResponse(
                scope,
                category,
                level,
                keyword,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0,
                0,
                true,
                true,
                false,
                null,
                List.of()
        );
    }

    private String normalizeCategoryFilter(String category) {
        if (category == null || category.isBlank() || "all".equals(category) || "전체".equals(category)) {
            return null;
        }
        if ("헬스".equals(category)) {
            return "근력";
        }
        return category;
    }

    private String normalizeLevelFilter(String level) {
        if (level == null || level.isBlank() || "all".equals(level) || "전체".equals(level)) {
            return null;
        }
        if ("beginner".equals(level)) {
            return "초급";
        }
        if ("입문".equals(level)) {
            return "초급";
        }
        if ("intermediate".equals(level)) {
            return "중급";
        }
        if ("advanced".equals(level)) {
            return "고급";
        }
        return level;
    }

    private List<ExerciseGroupResponse> groupByCategory(List<ExerciseCardResponse> cards) {
        Map<String, List<ExerciseCardResponse>> grouped = new LinkedHashMap<>();
        for (ExerciseCardResponse card : cards) {
            grouped.computeIfAbsent(card.category(), ignored -> new ArrayList<>()).add(card);
        }
        return grouped.entrySet().stream()
                .map(entry -> new ExerciseGroupResponse(
                        entry.getKey(),
                        ExerciseCardResponse.displayCategory(entry.getKey()),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();
    }
}
