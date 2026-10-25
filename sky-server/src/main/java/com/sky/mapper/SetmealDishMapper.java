package com.sky.mapper;


import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
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

    /**
     * 添加套餐菜品关系
     * @param setmealDishes
     */

    void save(List<SetmealDish> setmealDishes);

    /**
     * 删除套餐菜品对应关系
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{SetmealId}")
    void deleteBySetmealId(Long SetmealId);

    /**
     * 根据套餐Id查询菜品
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getDishBySetmealId(Long id);
}
