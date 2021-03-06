package org.etocrm.tagManager.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.etocrm.core.enums.BusinessEnum;
import org.etocrm.core.util.BeanUtils;
import org.etocrm.core.util.ResponseVO;
import org.etocrm.dynamicDataSource.util.RandomUtil;
import org.etocrm.kafkaServer.service.IKafkaProducerService;
import org.etocrm.tagManager.enums.TagErrorMsgEnum;
import org.etocrm.tagManager.enums.TagMethodEnum;
import org.etocrm.tagManager.mapper.ISysTagGroupMapper;
import org.etocrm.tagManager.mapper.ISysTagGroupRuleMapper;
import org.etocrm.tagManager.mapper.ISysTagMapper;
import org.etocrm.tagManager.model.DO.SysTagDO;
import org.etocrm.tagManager.model.DO.SysTagGroupDO;
import org.etocrm.tagManager.model.DO.SysTagGroupRuleDO;
import org.etocrm.tagManager.model.VO.tag.SysTagBrandsInfoVO;
import org.etocrm.tagManager.model.VO.tag.TagBrandsInfoVO;
import org.etocrm.tagManager.model.VO.tagGroup.*;
import org.etocrm.tagManager.service.ITagGroupService;
import org.etocrm.tagManager.util.BrandsInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
public class TagGroupServiceImpl extends ServiceImpl<ISysTagGroupMapper, SysTagGroupDO> implements ITagGroupService {

    @Autowired
    private ISysTagGroupMapper iSysTagGroupMapper;

    @Autowired
    @Qualifier("tagGroupServiceTransactionImpl")
    private TagGroupServiceTransactionImpl tagGroupServiceTransactionImpl;

    @Autowired
    private ISysTagGroupMapper sysTagGroupMapper;

    @Resource
    private BrandsInfoUtil brandsInfoUtil;

    @Autowired
    private ISysTagGroupRuleMapper sysTagGroupRuleMapper;

    @Autowired
    private IKafkaProducerService producerService;

    @Autowired
    private RandomUtil randomUtil;

    @Autowired
    private ISysTagMapper sysTagMapper;

    @Value("${CUSTOM.KAFKA.TOPIC.TAG_GROUP_USER_TOPIC}")
    private String tagGroupUserTopic;

    @Override
    public ResponseVO groupMethod(TagMethodEnum methodEnum, Object requestVO) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            switch (methodEnum) {
                case GROUP_ADD:
                    return tagGroupServiceTransactionImpl.addTagGroup((SysTagGroupAddVO) requestVO, brandsInfo);
                case GROUP_UPDATE:
                    return tagGroupServiceTransactionImpl.modifyTagGroup((SysTagGroupModifyVO) requestVO, brandsInfo);
                case GROUP_UPDATE_STATUS:
                    return tagGroupServiceTransactionImpl.updateStatus((SysTagGroupUpdateStatusVO) requestVO, brandsInfo);
                case GROUP_UPDATE_NAME:
                    return tagGroupServiceTransactionImpl.updateTagGroupName((SysTagGroupUpdateNameVO) requestVO, brandsInfo);
                case GROUP_COPY:
                    return tagGroupServiceTransactionImpl.copyTagGroup((Long) requestVO, brandsInfo);
                case GROUP_PREDICT:
                    return tagGroupServiceTransactionImpl.predict((SysTagGroupPredictVO) requestVO);
                case GROUP_GET_BY_ID:
                    return tagGroupServiceTransactionImpl.getGroupById((Long) requestVO, brandsInfo);
                case GROUP_LIST_PAGE:
                    return tagGroupServiceTransactionImpl.getListByPage((SysTagGroupQueryRequestVO) requestVO, brandsInfo);
                case GROUP_DELETE:
                    return tagGroupServiceTransactionImpl.deleteTagGroup((Long) requestVO, brandsInfo);
                case GROUP_DOWNLOAD:
                    return tagGroupServiceTransactionImpl.downLoadMemberPackage((String) requestVO, brandsInfo);
                default:
                    return ResponseVO.errorParams(TagErrorMsgEnum.OPERATION_FAILED.getMessage());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ResponseVO.errorParams(TagErrorMsgEnum.OPERATION_FAILED.getMessage());
    }


    /**
     * ????????????????????????
     *
     * @param tagGroupId
     * @return
     */
    @Override
    public ResponseVO recalculate(Long tagGroupId) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            //????????????????????????
            SysTagGroupDO tagGroupDO = this.getTagGroup(tagGroupId, brandsInfo);
            if (null == tagGroupDO) {
                return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
            }

            //todo 2021/1/8 ????????????????????????  ??????????????????????????????
            List<SysTagGroupRuleDO> sysTagGroupRuleDOS = sysTagGroupRuleMapper.selectList(new LambdaQueryWrapper<SysTagGroupRuleDO>()
                    .eq(SysTagGroupRuleDO::getTagGroupId, tagGroupId)
                    .eq(SysTagGroupRuleDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
            );
            SysTagGroupDO groupDO = new SysTagGroupDO();
            groupDO.setId(tagGroupId);
            if (CollectionUtil.isNotEmpty(sysTagGroupRuleDOS)) {
                Set<Long> collect = sysTagGroupRuleDOS.stream().map(SysTagGroupRuleDO::getTagId).collect(Collectors.toSet());
                if (!collect.isEmpty()) {
                    //??????????????????????????????????????????????????????
                    List<SysTagDO> sysTagDOS = sysTagMapper.selectBatchIds(collect);
                    for (SysTagDO sysTagDO : sysTagDOS) {
                        if (sysTagDO.getTagPropertyChangeExecuteStatus().equals(BusinessEnum.RULE_UNEXECUTED.getCode())) {
                            groupDO.setTagGroupRuleChangeExecuteStatus(BusinessEnum.UNEXECUTED.getCode());
                            sysTagGroupMapper.updateById(groupDO);
                            return ResponseVO.errorParams("???" + sysTagDO.getTagName() + "??????????????????????????????????????????");
                        }
                    }
                }
            }

            // ??????set ????????????
            SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
            countUserInfo.dealCountUserInfo();
            groupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));
            groupDO.setId(tagGroupId);
            sysTagGroupMapper.updateById(groupDO);

            producerService.sendMessage(tagGroupUserTopic,tagGroupId.toString(), randomUtil.getRandomIndex());
            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_RECALCULATE_ERROR.getMessage());
        }
    }

    /**
     * ??????????????????
     *
     * @param tagGroupId
     * @return
     */
    protected SysTagGroupDO getTagGroup(Long tagGroupId, TagBrandsInfoVO brandsInfo) {
        return sysTagGroupMapper.selectOne(new LambdaQueryWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getId, tagGroupId)
                .eq(SysTagGroupDO::getBrandsId, brandsInfo.getBrandsId())
                .eq(SysTagGroupDO::getOrgId, brandsInfo.getOrgId())
        );
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    @Override
    public ResponseVO<List<SysTagGroupListResponseVO>> getEnableList() {
        TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
        if (null != brandsInfo.getResponseVO()) {
            return brandsInfo.getResponseVO();
        }
        List<SysTagGroupListResponseVO> responseVOList = new ArrayList<>();

        List<SysTagGroupDO> ruleDOList = sysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .eq(SysTagGroupDO::getTagGroupStatus, BusinessEnum.USING.getCode())
                .eq(SysTagGroupDO::getBrandsId, brandsInfo.getBrandsId())
                .eq(SysTagGroupDO::getOrgId, brandsInfo.getOrgId())
        );
        SysTagGroupListResponseVO data;
        for (SysTagGroupDO groupDO : ruleDOList) {
            data = new SysTagGroupListResponseVO();
            BeanUtils.copyPropertiesIgnoreNull(groupDO, data);
            responseVOList.add(data);
        }
        return ResponseVO.success(responseVOList);
    }

    /**
     * ???????????????????????????????????????
     * ???????????????????????? ?????????-????????????-
     *
     * @return
     */
    @Override
    public List<SysTagGroupDO> getTagGroupListByDataManager(String brandAndOrgId) {
        return tagGroupServiceTransactionImpl.getTagGroupListByDataManager(brandAndOrgId);
    }

    /**
     * ????????????id ,????????????
     *
     * @param tagGroupDO
     * @return
     */
    @Override
    public Set<Long> getUserListByGroupId(SysTagGroupDO tagGroupDO) {
        return tagGroupServiceTransactionImpl.getUserListByGroupId(tagGroupDO);
    }

    @Override
    public ResponseVO getGroupRuleByTagIds(SysTagBrandsInfoVO brandsInfoVO) {
        //???????????????????????????tagId
        List<SysTagGroupDO> groupDOList = iSysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .select(SysTagGroupDO::getId)
                .eq(SysTagGroupDO::getBrandsId, brandsInfoVO.getBrandsId())
                .eq(SysTagGroupDO::getOrgId, brandsInfoVO.getOrgId())
        );
        if (CollectionUtil.isEmpty(groupDOList)) {
            return ResponseVO.success(new ArrayList<>());
        }
        //????????????tagId?????????????????????
        Set<Long> tagIds = groupDOList.stream().map(groupDO -> groupDO.getId()).collect(Collectors.toSet());
        LambdaQueryWrapper<SysTagGroupRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTagGroupRuleDO::getDeleted, BusinessEnum.NOTDELETED.getCode());
        wrapper.in(SysTagGroupRuleDO::getTagId, tagIds);
        return ResponseVO.success(sysTagGroupRuleMapper.selectList(wrapper));
    }

    @Override
    public ResponseVO editGroupRule(SysTagGroupRuleVO sysTagGroupRuleVO) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            return tagGroupServiceTransactionImpl.editGroupRule(sysTagGroupRuleVO, brandsInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ResponseVO.errorParams(TagErrorMsgEnum.OPERATION_FAILED.getMessage());
    }

    @Override
    public ResponseVO<SysTagGroupRuleGetResponseVO> getGroupRuleByGroupId(Long tagGroupId) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            return tagGroupServiceTransactionImpl.getGroupRuleById(tagGroupId, brandsInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ResponseVO.errorParams(TagErrorMsgEnum.OPERATION_FAILED.getMessage());
    }

    @Override
    public SysTagGroupDO getTagGroupById(Long tagGroupId) {
        return iSysTagGroupMapper.selectById(tagGroupId);
    }

    @Override
    public ResponseVO updateResetTagGroupById(Long tagGroupId) {
        SysTagGroupDO tagGroupUpdateDO = new SysTagGroupDO();
        tagGroupUpdateDO.setTagGroupType(BusinessEnum.TAG_GROUP_TYPE_STATIC.getCode());
        LambdaUpdateWrapper<SysTagGroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        // ?????????????????????eq id??? ??????????????????????????????
        updateWrapper.eq(SysTagGroupDO::getId,tagGroupId);
        updateWrapper.set(SysTagGroupDO::getTagGroupRestDate, null);
        iSysTagGroupMapper.update(tagGroupUpdateDO, updateWrapper);
        return ResponseVO.success();
    }

}
