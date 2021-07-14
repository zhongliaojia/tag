package org.etocrm.tagManager.mapper.mat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etocrm.tagManager.model.DO.mat.MetadataPropertyDO;

/**
 * <p>
 * MAT系统元数据属性映射表  Mapper 接口
 * </p>
 */
@Mapper
public interface IMatMetadataPropertyMapper extends BaseMapper<MetadataPropertyDO> {

}