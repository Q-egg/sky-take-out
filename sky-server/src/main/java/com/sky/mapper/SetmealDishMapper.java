package com.sky.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {


    /**
     *根据菜品ids查询套餐
     */
    List<Long> getSetmealIdByDishIds(List<Long> dishIds);

    @Select("select setmeal_id from setmeal_dish where id = #{id}")
    Long getSetmealIdByDishId(Long id);
}
