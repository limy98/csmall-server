package cn.tedu.csmall.product.webapi.service;

import cn.tedu.csmall.common.ex.ServiceException;
import cn.tedu.csmall.common.web.State;
import cn.tedu.csmall.pojo.dto.CategoryAddNewDTO;
import cn.tedu.csmall.pojo.entity.Category;
import cn.tedu.csmall.pojo.vo.CategorySimpleListItemVO;
import cn.tedu.csmall.pojo.vo.CategorySimpleVO;
import cn.tedu.csmall.product.service.ICategoryService;
import cn.tedu.csmall.product.webapi.mapper.CategoryMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements ICategoryService {

    @Autowired
    CategoryMapper categoryMapper;

    @Override
    public void addNew(CategoryAddNewDTO categoryAddNewDTO) {
        // 从参数中取出尝试添加的类别的名称
        String name = categoryAddNewDTO.getName();
        // 调用categoryMapper.getByName()方法查询
        CategorySimpleVO queryResult = categoryMapper.getByName(name);
        // 判断查询结果是否不为null
        if (queryResult != null) {
            // 是：抛出ServiceException
            throw new ServiceException(State.ERR_CATEGORY_NAME_DUPLICATE,
                    "添加类别失败，名称（" + name + "）已存在！");
        }

        // 从参数中取出父级类别的id：parentId
        Long parentId = categoryAddNewDTO.getParentId();
        // 判断parentId是否为0，当前尝试新增的类别的depth默认为1
        Integer depth = 1;
        CategorySimpleVO parentCategory = null;
        if (parentId != 0) {
            // 否：此次尝试添加的不是一级类别，则应该存在父级类别，调用categoryMapper.getById()方法查询父级类别的信息
            parentCategory = categoryMapper.getById(parentId);
            // -- 判断查询结果是否为null
            if (parentCategory == null) {
                // -- 是：抛出ServiceException
                throw new ServiceException(State.ERR_CATEGORY_NOT_FOUND,
                        "添加类别失败，父级类别不存在！");
            }
            // -- 否：当前depth >>> 父级depth + 1
            depth = parentCategory.getDepth() + 1;
        }

        // 创建Category对象
        Category category = new Category();
        // 调用BeanUtils.copyProperties()将参数对象中的属性值复制到Category对象中
        BeanUtils.copyProperties(categoryAddNewDTO, category);
        // 补全Category对象中的属性值：depth >>> 前序运算结果
        category.setDepth(depth);
        // 补全Category对象中的属性值：enable >>> 1（默认即启用）
        category.setEnable(1);
        // 补全Category对象中的属性值：isParent >>> 0
        category.setIsParent(0);
        // 补全Category对象中的属性值：gmtCreate, gmtModified >>> LocalDateTime.now()
        LocalDateTime now = LocalDateTime.now();
        category.setGmtCreate(now);
        category.setGmtModified(now);
        // 调用categoryMapper.insert(Category)插入类别数据，获取返回的受影响的行数
        int rows = categoryMapper.insert(category);
        //  判断返回的受影响的行数是否不为1
        if (rows != 1) {
            // 是：抛出ServiceException
            throw new ServiceException(State.ERR_INSERT,
                    "添加类别失败，服务器忙（" + State.ERR_INSERT.getValue() + "），请稍后再次尝试！");
        }

        // 判断父级类别的isParent是否为0
        // 以下判断条件有部分多余，但不会报错
        if (parentId != 0 && parentCategory != null && parentCategory.getIsParent() == 0) {
            // 是：调用categoryMapper.updateIsParentById()方法，将父级类别的isParent修改为1，获取返回的受影响的行数
            rows = categoryMapper.updateIsParentById(parentId, 1);
            // 判断返回的受影响的行数是否不为1
            if (rows != 1) {
                // 是：抛出ServiceException
                throw new ServiceException(State.ERR_UPDATE,
                        "添加类别失败，服务器忙（" + State.ERR_UPDATE.getValue() + "），请稍后再次尝试！");
            }
        }
    }


    @Override
    public List<CategorySimpleListItemVO> listByParentId(Long parentId) {
        return categoryMapper.listByParentId(parentId);
    }


}
