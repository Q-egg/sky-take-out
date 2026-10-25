package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //新增套餐
        setmealMapper.save(setmeal);
        Long id = setmeal.getId();
        //新增套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(id);});
        setmealDishMapper.save(setmealDishes);
    }


    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        long total = page.getTotal();
        List<Setmeal> result = page.getResult();
        return new PageResult(total, result);

    }


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO queryById(Long id){
        //查询套餐数据
        Setmeal setmeal = setmealMapper.queryById(id);
        //查询菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //修改套餐
        setmealMapper.update(setmeal);

        //先删除套餐菜品对应关系，再添加菜品
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmeal.getId());});
            setmealDishMapper.save(setmealDishes);
        }
    }


    /**
     * 起售停售套餐
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id)
    {
        //判断套餐中是否有未起售的菜品
        Integer dish  = setmealDishMapper.queryStatusWithDish(id);
        if(dish != null && dish > 0){
            throw new SetmealEnableFailedException(
                    MessageConstant.SETMEAL_ENABLE_FAILED
            );
        }
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);

        //设置销售状态
        setmealMapper.startOrStop(setmeal,id);
    }

    /**
     * 根据ids批量删除套餐数据
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids)
    {
        //删除套餐数据
        setmealMapper.deleteByIds(ids);

        //删除套餐菜品关系数据
        setmealDishMapper.deleteBySetmealIds(ids);

    }


}
