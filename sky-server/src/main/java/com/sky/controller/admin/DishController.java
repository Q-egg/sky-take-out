package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品 {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {

        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }


    /**
     * 删除菜品
     */
    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam List<Long> ids){

        dishService.deleteBatch(ids);

        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 根据id修改菜品
     */
    @PutMapping
    @ApiOperation("根据id修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {

        dishService.updateWithFlavor(dishDTO);

        cleanCache("dish_*");


        return Result.success();
    }

    /**
     * 根据id起售停售菜品
     */
    @PostMapping("/status/{status}")
    @ApiOperation("起售停售菜品")
     public Result startOrStop(@PathVariable Integer status, Long id){
         dishService.startOrStop(status,id);

       cleanCache("dish_*");

         return Result.success();
     }

    /**
     * 根据分类id查询菜品
     * @param
     * @return
     */
     @GetMapping("/list")
     @ApiOperation("根据分类ID查询菜品")
     public Result<List<Dish>> queryListByCategoryId(Dish dish) {
             List<Dish> dishlist = dishService.queryListByCategoryId(dish);
             return Result.success(dishlist);
     }


    /**
     * 统一清理缓存数据
     * @param pattern
     */

    private void cleanCache(String pattern){
         Set keys = redisTemplate.keys(pattern);
         redisTemplate.delete(keys);

     }
}
