package com.credit.system.dto.mapper;

/**
 * 实体到 DTO 的转换映射器接口。
 * <p>
 * 统一管理 Entity → DTO 的转换逻辑，避免转换代码散落在 DTO 类和 Controller 中。
 * </p>
 *
 * @param <E> 实体类型
 * @param <D> DTO 类型
 */
public interface EntityMapper<E, D> {

    /**
     * 将实体转换为 DTO。
     *
     * @param entity 实体对象
     * @return DTO 对象
     */
    D toDto(E entity);

    /**
     * 将 DTO 转换为实体（用于创建/更新请求）。
     *
     * @param dto DTO 对象
     * @return 实体对象
     */
    E toEntity(D dto);
}
