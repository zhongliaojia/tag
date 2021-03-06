package org.etocrm.tagManager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.etocrm.core.enums.BusinessEnum;
import org.etocrm.core.enums.ResponseEnum;
import org.etocrm.core.util.BeanUtils;
import org.etocrm.core.util.ParamDeal;
import org.etocrm.core.util.ResponseVO;
import org.etocrm.core.util.VoParameterUtils;
import org.etocrm.dynamicDataSource.util.BasePage;
import org.etocrm.dynamicDataSource.util.RandomUtil;
import org.etocrm.dynamicDataSource.util.RedisUtil;
import org.etocrm.kafkaServer.service.IKafkaProducerService;
import org.etocrm.tagManager.api.IAuthenticationService;
import org.etocrm.tagManager.api.IDataManagerService;
import org.etocrm.tagManager.constant.TagConstant;
import org.etocrm.tagManager.enums.TagDictEnum;
import org.etocrm.tagManager.enums.TagErrorMsgEnum;
import org.etocrm.tagManager.mapper.*;
import org.etocrm.tagManager.model.DO.*;
import org.etocrm.tagManager.model.VO.DictFindAllVO;
import org.etocrm.tagManager.model.VO.SysDictVO;
import org.etocrm.tagManager.model.VO.SysUserAllVO;
import org.etocrm.tagManager.model.VO.SysUserOutVO;
import org.etocrm.tagManager.model.VO.tag.TagBrandsInfoVO;
import org.etocrm.tagManager.model.VO.tagGroup.*;
import org.etocrm.tagManager.service.ITagGroupRuleService;
import org.etocrm.tagManager.service.ITagGroupUserService;
import org.etocrm.tagManager.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TagGroupServiceTransactionImpl extends ServiceImpl<ISysTagGroupMapper, SysTagGroupDO> {

    @Autowired
    private ISysTagGroupMapper sysTagGroupMapper;

    @Autowired
    private ISysTagGroupRuleMapper sysTagGroupRuleMapper;

    @Autowired
    private ITagGroupRuleService tagGroupRuleService;

    @Autowired
    private ISysTagMapper sysTagMapper;

    @Autowired
    private ISysTagPropertyMapper sysTagPropertyMapper;

    @Autowired
    private IDataManagerService dataManagerService;

    @Autowired
    private ITagGroupUserService tagGroupUserService;

    @Autowired
    private ISysTagPropertyUserMapper sysTagPropertyUserMapper;

    @Autowired
    private ISysTagGroupUserMapper sysTagGroupUserMapper;

    @Autowired
    private IAuthenticationService authenticationService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IKafkaProducerService producerService;

    @Autowired
    private RandomUtil randomUtil;

    @Value("${CUSTOM.KAFKA.TOPIC.TAG_GROUP_USER_TOPIC}")
    private String tagGroupUserTopic;


    /**
     * ????????????
     *
     * @param tagGroupAddVO
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public ResponseVO addTagGroup(SysTagGroupAddVO tagGroupAddVO, TagBrandsInfoVO brandsInfo) {
        // 1.????????????
        if (BusinessEnum.TAG_GROUP_START_ASSIGN_DATE.getCode().equals(tagGroupAddVO.getTagGroupStartType()) && null == tagGroupAddVO.getTagGroupStartTime()) {
            return ResponseVO.errorParams("?????????????????????");
        }

        //?????????????????????????????????????????????????????????????????????????????????
        if (null != tagGroupAddVO.getTagGroupRestDate()) {
            if (BusinessEnum.TAG_GROUP_START_ASSIGN_DATE.getCode().equals(tagGroupAddVO.getTagGroupStartType()) && DateUtil.between(tagGroupAddVO.getTagGroupStartTime(), tagGroupAddVO.getTagGroupRestDate(), DateUnit.DAY, false) < 0) {
                return ResponseVO.errorParams("????????????????????????????????????");
            }
            if (BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(tagGroupAddVO.getTagGroupStartType()) && DateUtil.between(new Date(), tagGroupAddVO.getTagGroupRestDate(), DateUnit.DAY, false) < 0) {
                return ResponseVO.errorParams("?????????????????????????????????");
            }
        }

        //????????????????????????
        tagGroupAddVO.setTagGroupName(tagGroupAddVO.getTagGroupName().trim());

        // 2.????????????????????????
        if (getByTagGroupNameCount(tagGroupAddVO.getTagGroupName(), null, brandsInfo) > 0) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NAME_EXISTS.getMessage());
        }

        // 3.add group ???
        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        BeanUtils.copyPropertiesIgnoreNull(tagGroupAddVO, sysTagGroupDO);
        // ???????????????????????????
        sysTagGroupDO.setTagGroupStatus(BusinessEnum.USING.getCode());
        sysTagGroupDO.setBrandsId(brandsInfo.getBrandsId());
        sysTagGroupDO.setOrgId(brandsInfo.getOrgId());
        sysTagGroupMapper.insert(sysTagGroupDO);

        return ResponseVO.success(sysTagGroupDO.getId());
    }

    /**
     * ????????????
     *
     * @param tagGroupModifyVO
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public ResponseVO modifyTagGroup(SysTagGroupModifyVO tagGroupModifyVO, TagBrandsInfoVO brandsInfo) {
        // ????????????
        if (BusinessEnum.TAG_GROUP_START_ASSIGN_DATE.getCode().equals(tagGroupModifyVO.getTagGroupStartType()) && null == tagGroupModifyVO.getTagGroupStartTime()) {
            return ResponseVO.errorParams("?????????????????????");
        }

        //?????????????????????????????????????????????????????????????????????????????????
        if (null != tagGroupModifyVO.getTagGroupRestDate()) {
            if (BusinessEnum.TAG_GROUP_START_ASSIGN_DATE.getCode().equals(tagGroupModifyVO.getTagGroupStartType()) && DateUtil.between(tagGroupModifyVO.getTagGroupStartTime(), tagGroupModifyVO.getTagGroupRestDate(), DateUnit.DAY, false) < 0) {
                return ResponseVO.errorParams("????????????????????????????????????");
            }
            if (BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(tagGroupModifyVO.getTagGroupStartType()) && DateUtil.between(new Date(), tagGroupModifyVO.getTagGroupRestDate(), DateUnit.DAY, false) < 0) {
                return ResponseVO.errorParams("?????????????????????????????????");
            }
        }

        //????????????????????????
        SysTagGroupDO tagGroupDOFind = getTagGroup(tagGroupModifyVO.getId(), brandsInfo);
        if (null == tagGroupDOFind) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }

        //?????????????????????????????????
        if (checkModifyDepending(tagGroupModifyVO.getId())) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_USING_UPDATE_FAILED.getMessage());
        }

        //????????????????????????
        tagGroupModifyVO.setTagGroupName(tagGroupModifyVO.getTagGroupName().trim());

        //????????????????????????
        if (getByTagGroupNameCount(tagGroupModifyVO.getTagGroupName(), tagGroupModifyVO.getId(), brandsInfo) > 0) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NAME_EXISTS.getMessage());
        }

        //update group ???
        LambdaUpdateWrapper<SysTagGroupDO> updateWrapper = new LambdaUpdateWrapper<SysTagGroupDO>();
        updateWrapper.eq(SysTagGroupDO::getId, tagGroupModifyVO.getId());

        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        BeanUtils.copyPropertiesIgnoreNull(tagGroupModifyVO, sysTagGroupDO);
        sysTagGroupDO.setBrandsId(brandsInfo.getBrandsId());
        sysTagGroupDO.setOrgId(brandsInfo.getOrgId());
        //????????????????????????????????????
        if (BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(tagGroupModifyVO.getTagGroupStartType())) {
            updateWrapper.set(SysTagGroupDO::getTagGroupStartTime, null);
        }

        boolean calculate = false;
        // TODO: 2020/9/25  ????????????????????????????????????????????????
        //  (????????? ???????????? ) & ??????  & ????????????  &  ?????????  ???????????????
        if (!tagGroupDOFind.getTagGroupStartType().equals(sysTagGroupDO.getTagGroupStartType())
                && BusinessEnum.USING.getCode().equals(tagGroupDOFind.getTagGroupStatus())
//                && BusinessEnum.TAG_GROUP_TYPE_STATIC.getCode().equals(sysTagGroupDO.getTagGroupType())
                && BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(sysTagGroupDO.getTagGroupStartType())
                && getRuleCount(tagGroupModifyVO.getId()) > 0) {

            // ??????set ????????????
            SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
            countUserInfo.dealCountUserInfo();
            sysTagGroupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));

            calculate = true;
        }

        sysTagGroupMapper.update(sysTagGroupDO, updateWrapper);
        if (calculate) {
            //????????????
            producerService.sendMessage(tagGroupUserTopic, String.valueOf(sysTagGroupDO.getId()), randomUtil.getRandomIndex());
        }
        return ResponseVO.success();

    }

    /**
     * ??????????????????
     *
     * @param tagGroupId
     * @return
     */
    private Integer getRuleCount(Long tagGroupId) {
        return sysTagGroupRuleMapper.selectCount(new LambdaQueryWrapper<SysTagGroupRuleDO>()
                .eq(SysTagGroupRuleDO::getTagGroupId, tagGroupId)
                .eq(SysTagGroupRuleDO::getDeleted, BusinessEnum.NOTDELETED.getCode()));
    }

    /**
     * ????????????
     *
     * @param tagGroupId
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public ResponseVO copyTagGroup(Long tagGroupId, TagBrandsInfoVO brandsInfo) {
        SysTagGroupDO srcSysTagGroupDO = getTagGroup(tagGroupId, brandsInfo);
        if (null == srcSysTagGroupDO) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }
        String newTagGroupName = srcSysTagGroupDO.getTagGroupName() + "-??????";
        if (newTagGroupName.length() > 20) {
            return ResponseVO.errorParams("????????????????????? " + newTagGroupName + " ????????????????????????????????????????????????????????????");
        }
        List<SysTagGroupDO> sysTagGroupDOS = sysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getTagGroupName, newTagGroupName)
                .eq(SysTagGroupDO::getOrgId, brandsInfo.getOrgId())
                .eq(SysTagGroupDO::getBrandsId, brandsInfo.getBrandsId())
        );

        if (CollUtil.isNotEmpty(sysTagGroupDOS)) {
            return ResponseVO.errorParams("????????????????????? " + newTagGroupName + " ??????????????????????????????????????????????????????");
        }
        //2.copy ??????name ????????? -??????
        SysTagGroupDO targetSysTagGroupDO = new SysTagGroupDO();
        BeanUtils.copyPropertiesIgnoreNull(srcSysTagGroupDO, targetSysTagGroupDO);
        targetSysTagGroupDO.setId(null);
        targetSysTagGroupDO.setTagGroupName(targetSysTagGroupDO.getTagGroupName() + "-??????");

        //?????????????????????
        if (StringUtils.isNotBlank(targetSysTagGroupDO.getCountUserInfo())) {
            SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
            countUserInfo.copyCountUserInfo();
            targetSysTagGroupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));
        }

        //??????????????? ????????????????????????????????? ????????????
        targetSysTagGroupDO.setTagGroupSplitCount(0L);
        targetSysTagGroupDO.setSonCountInfo(null);
        sysTagGroupMapper.insert(targetSysTagGroupDO);

        //3.?????????????????????
        List<SysTagGroupRuleRequestVO> srcRule = tagGroupRuleService.getCopyRule(tagGroupId);

        //4.srcRule to targetRule and save rule
        this.saveGroupRule(targetSysTagGroupDO, srcRule, false);
        Long targetGroupId = targetSysTagGroupDO.getId();
        //5 ????????????????????????   todo ??????????????????
        List<SysTagGroupUserDO> groupUserDOList = sysTagGroupUserMapper.selectList(new LambdaQueryWrapper<SysTagGroupUserDO>()
                .select(SysTagGroupUserDO::getUserId)
                .eq(SysTagGroupUserDO::getTagGroupId, tagGroupId));
        List<SysTagGroupUserPO> groupUserPOList = groupUserDOList.stream().map(item -> {
            SysTagGroupUserPO userPO = new SysTagGroupUserPO();
            userPO.setUserId(item.getUserId());
            userPO.setTagGroupId(targetGroupId);
            return userPO;
        }).collect(Collectors.toList());

        if (CollUtil.isNotEmpty(groupUserPOList)) {
            tagGroupUserService.asyncSaveBatchGroupUser(targetGroupId, groupUserPOList);
        }
        return ResponseVO.success();
    }

    @Transactional(rollbackFor = {Exception.class})
    public ResponseVO updateStatus(SysTagGroupUpdateStatusVO updateStatusVO, TagBrandsInfoVO brandsInfo) {

        //1.????????????????????????
        SysTagGroupDO sysTagGroupDOFind = getTagGroup(updateStatusVO.getTagGroupId(), brandsInfo);
        if (null == sysTagGroupDOFind) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }
        if (sysTagGroupDOFind.getTagGroupStatus().equals(BusinessEnum.USING.getCode()) && BusinessEnum.NOTUSE.getCode().equals(updateStatusVO.getTagGroupStatus())) {
            //2.????????????
            if (checkModifyDepending(updateStatusVO.getTagGroupId())) {
                return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_USING_UPDATE_FAILED.getMessage());
            }
        }

        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        sysTagGroupDO.setId(updateStatusVO.getTagGroupId());

        boolean calculate = false;
        // TODO: 2020/9/25 ??????????????????  ???????????????  ???????????? ????????????
        if (!sysTagGroupDOFind.getTagGroupStatus().equals(updateStatusVO.getTagGroupStatus())
                && BusinessEnum.USING.getCode().equals(updateStatusVO.getTagGroupStatus())
                && BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(sysTagGroupDOFind.getTagGroupStartType())
                && getRuleCount(updateStatusVO.getTagGroupId()) > 0
        ) {
            //todo 2021/1/8 ????????????????????????  ??????????????????????????????
            List<SysTagGroupRuleDO> sysTagGroupRuleDOS = sysTagGroupRuleMapper.selectList(new LambdaQueryWrapper<SysTagGroupRuleDO>()
                    .eq(SysTagGroupRuleDO::getTagGroupId, sysTagGroupDOFind.getId())
                    .eq(SysTagGroupRuleDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
            );
            if (CollectionUtil.isNotEmpty(sysTagGroupRuleDOS)) {
                Set<Long> collect = sysTagGroupRuleDOS.stream().map(SysTagGroupRuleDO::getTagId).collect(Collectors.toSet());
                if (!collect.isEmpty()) {
                    //??????????????????????????????????????????????????????
                    List<SysTagDO> sysTagDOS = sysTagMapper.selectBatchIds(collect);
                    for (SysTagDO sysTagDO : sysTagDOS) {
                        if (sysTagDO.getTagPropertyChangeExecuteStatus().equals(BusinessEnum.RULE_UNEXECUTED.getCode())) {
                            sysTagGroupDO.setTagGroupRuleChangeExecuteStatus(BusinessEnum.UNEXECUTED.getCode());
                            sysTagGroupMapper.updateById(sysTagGroupDO);
                            return ResponseVO.errorParams("???" + sysTagDO.getTagName() + "??????????????????????????????????????????");
                        }
                    }
                }
            }
            // ??????set ????????????
            SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
            countUserInfo.dealCountUserInfo();
            sysTagGroupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));
            calculate = true;
        }

        sysTagGroupDO.setTagGroupStatus(updateStatusVO.getTagGroupStatus());
        sysTagGroupMapper.updateById(sysTagGroupDO);

        if (calculate) {
            producerService.sendMessage(tagGroupUserTopic, String.valueOf(updateStatusVO.getTagGroupId()), randomUtil.getRandomIndex());
        }
        return ResponseVO.success();
    }

    /**
     * ??????????????????????????????
     *
     * @param queryRequestVO
     * @return
     */
    public ResponseVO<BasePage<ListPageSysTagGroupQueryResponseVO>> getListByPage(SysTagGroupQueryRequestVO queryRequestVO, TagBrandsInfoVO brandsInfo) throws IllegalAccessException {
        log.info("=============enter,beginTime:{}", System.currentTimeMillis());
        ParamDeal.setStringNullValue(queryRequestVO);
        IPage<SysTagGroupDO> page = new Page<>(VoParameterUtils.getCurrent(queryRequestVO.getCurrent()), VoParameterUtils.getSize(queryRequestVO.getSize()));

        LambdaQueryWrapper<SysTagGroupDO> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        objectLambdaQueryWrapper.eq(SysTagGroupDO::getOrgId, brandsInfo.getOrgId())
                .eq(SysTagGroupDO::getBrandsId, brandsInfo.getBrandsId());

        if (StringUtils.isNotBlank(queryRequestVO.getTagGroupName())) {
            objectLambdaQueryWrapper.like(SysTagGroupDO::getTagGroupName, queryRequestVO.getTagGroupName());
        }
        if (StringUtils.isNotBlank(queryRequestVO.getCreatedByName())) {
            List createUserIdList = null;
            SysUserAllVO sysUserAllVO = new SysUserAllVO();
            sysUserAllVO.setUserName(queryRequestVO.getCreatedByName());
            ResponseVO<List<SysUserOutVO>> userAll = authenticationService.findUserAll(sysUserAllVO);
            if (userAll.getCode() != 0) {
                return ResponseVO.errorParams(TagErrorMsgEnum.SELECT_ERROR.getMessage());
            }
            if (CollUtil.isEmpty(userAll.getData())) {
                //do ????????????
                return ResponseVO.success(new BasePage<>(new ArrayList<>()));
            }
            createUserIdList = userAll.getData().stream().map(user -> user.getId()).collect(Collectors.toList());
            objectLambdaQueryWrapper.in(SysTagGroupDO::getCreatedBy, createUserIdList);
        }
        if (null != queryRequestVO.getStartTime()) {
            objectLambdaQueryWrapper.ge(SysTagGroupDO::getCreatedTime, queryRequestVO.getStartTime());
        }
        // ?????????????????????????????????+1 .????????????????????? ?????????
        if (null != queryRequestVO.getEndTime()) {
            //?????????1
            Date endTime = DateUtil.offsetDay(queryRequestVO.getEndTime(), 1);
            objectLambdaQueryWrapper.lt(SysTagGroupDO::getCreatedTime, endTime);
        }
        if (null != queryRequestVO.getTagGroupType()) {
            objectLambdaQueryWrapper.eq(SysTagGroupDO::getTagGroupType, queryRequestVO.getTagGroupType());
        }
        if (null != queryRequestVO.getTagGroupStatus()) {
            objectLambdaQueryWrapper.eq(SysTagGroupDO::getTagGroupStatus, queryRequestVO.getTagGroupStatus());
        }
        objectLambdaQueryWrapper.eq(SysTagGroupDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .orderByDesc(SysTagGroupDO::getId);
        log.info("=============enter,before select time:{}", System.currentTimeMillis());
        IPage<SysTagGroupDO> sysTagClassesDOIPage = sysTagGroupMapper.selectPage(page, objectLambdaQueryWrapper);
        log.info("=============enter,after select time:{}", System.currentTimeMillis());
        List<ListPageSysTagGroupQueryResponseVO> list = new ArrayList<>();
        List<SysTagGroupDO> records = sysTagClassesDOIPage.getRecords();

        ListPageSysTagGroupQueryResponseVO tagGroupQueryRequestVO;
        for (SysTagGroupDO record : records) {
            tagGroupQueryRequestVO = this.dealTagGroupInfo(record);

            list.add(tagGroupQueryRequestVO);
        }
        log.info("=============enter,for end select time:{}", System.currentTimeMillis());

        BasePage<ListPageSysTagGroupQueryResponseVO> objectBasePage = new BasePage<>(sysTagClassesDOIPage);
        objectBasePage.setRecords(list);
        return ResponseVO.success(objectBasePage);
    }

    private ListPageSysTagGroupQueryResponseVO dealTagGroupInfo(SysTagGroupDO record) {
        ListPageSysTagGroupQueryResponseVO tagGroupQueryRequestVO = new ListPageSysTagGroupQueryResponseVO();
        BeanUtils.copyPropertiesIgnoreNull(record, tagGroupQueryRequestVO);

        //????????????
        String countUserInfoStr = record.getCountUserInfo();
        if (StringUtils.isNotBlank(countUserInfoStr)) {
            SysTagGroupCountUserInfo countUserInfoObj = JSON.parseObject(countUserInfoStr, SysTagGroupCountUserInfo.class);
            if (null != countUserInfoObj) {
                tagGroupQueryRequestVO.setCountUser(countUserInfoObj.getCountUser());
                tagGroupQueryRequestVO.setCountMemberId(countUserInfoObj.getCountMemberId());
                tagGroupQueryRequestVO.setCountMobileId(countUserInfoObj.getCountMobileId());
                tagGroupQueryRequestVO.setCountUnionID(countUserInfoObj.getCountUnionID());
            }
        }

        //???????????????
        tagGroupQueryRequestVO.setSonCountUser(record.getTagGroupSplitCount());

        // ???????????????
        String sonCountInfoStr = record.getSonCountInfo();
        if (StringUtils.isNotBlank(sonCountInfoStr)) {
            tagGroupQueryRequestVO.setSonCountUserInfo(JSON.parseArray(sonCountInfoStr, SysTagGroupSonUserInfo.class));
        }

        //????????????
        tagGroupQueryRequestVO.setCreatedByName(getUserName(record.getCreatedBy()));
        tagGroupQueryRequestVO.setCreateTime(DateUtil.format(record.getCreatedTime(), DatePattern.NORM_DATETIME_PATTERN));
        tagGroupQueryRequestVO.setUpdatedTime(DateUtil.format(record.getUpdatedTime(), DatePattern.NORM_DATETIME_PATTERN));

        return tagGroupQueryRequestVO;
    }

    private String getUserName(Integer userId) {
        if (null != userId) {
            ResponseVO<SysUserOutVO> userById = authenticationService.getUserById(userId);
            if (null != userById && null != userById.getData()) {
                return (userById.getData()).getUserName();
            }
        }
        return "";
    }

    /**
     * ????????????
     *
     * @param predictVO
     * @return
     */
    public ResponseVO predict(SysTagGroupPredictVO predictVO) {
        PredictResponseVO resultData = new PredictResponseVO();

        //????????????id ????????????code
        HashMap<Long, String> relationshipMap = getDictCodeMap(TagDictEnum.TAG_RELATIONAL_OPERATION.getCode());
        HashMap<Long, String> logicalOperationMap = getDictCodeMap(TagDictEnum.GROUP_OPERATORS.getCode());
        if (relationshipMap.keySet().size() < 1 || logicalOperationMap.keySet().size() < 1) {
            return ResponseVO.errorParams("????????????????????????");
        }
        //???????????? ???/???
        String groupRelationCode = relationshipMap.get(predictVO.getTagGroupRuleRelationshipId());
        //????????????list
        Set<Long> userIdList = new HashSet<>();
        Set<Long> groupInnerUserIdList;
        //????????????????????????list
        List<SysTagGroupRuleRequestVO> ruleRequestVOS = predictVO.getRule();
        int groupCount = 0;
        for (SysTagGroupRuleRequestVO ruleVO : ruleRequestVOS) {
            //??????????????? ??? ??????????????? ???????????????????????? ????????????????????????
            if (groupCount > 1 && StringUtils.equals(TagDictEnum.TAG_RELATIONAL_OPERATION_AND.getCode(), groupRelationCode) && CollUtil.isEmpty(userIdList)) {
                resultData.setCount(0);
                return ResponseVO.success(resultData);
            }

            //???????????????/???code
            String groupInnerRelationshipCode = relationshipMap.get(ruleVO.getTagGroupRuleRelationshipId());
            if (StrUtil.isBlank(groupRelationCode)) {
                return ResponseVO.errorParams("????????????????????????");
            }
            groupInnerUserIdList = this.getUserIdListByRuleVOList(ruleVO.getTagGroupRule(), groupInnerRelationshipCode, logicalOperationMap);

            //?????????????????????/???
            if (0 == groupCount) {
                userIdList = groupInnerUserIdList;
            } else {
                dealListByRelationCode(userIdList, groupInnerUserIdList, groupRelationCode);
            }

            groupCount++;
        }
        resultData.setCount(userIdList.size());
        return ResponseVO.success(resultData);
    }

    /**
     * ????????????
     *
     * @param tagGroupId
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public ResponseVO deleteTagGroup(Long tagGroupId, TagBrandsInfoVO brandsInfo) {
        //????????????????????????
        SysTagGroupDO tagGroupDOFind = this.getTagGroup(tagGroupId, brandsInfo);
        if (null == tagGroupDOFind) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }

        //????????????
        if (checkDeleteDepending(tagGroupId)) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_USING_DELETE_FAILED.getMessage());
        }

        //????????????
        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        sysTagGroupDO.setId(tagGroupId);
        sysTagGroupDO.setDeleted(BusinessEnum.DELETED.getCode());
        sysTagGroupMapper.updateById(sysTagGroupDO);

        //??????????????????
        SysTagGroupRuleDO ruleDO = new SysTagGroupRuleDO();
        ruleDO.setDeleted(BusinessEnum.DELETED.getCode());
        sysTagGroupRuleMapper.update(ruleDO, new LambdaUpdateWrapper<SysTagGroupRuleDO>()
                .eq(SysTagGroupRuleDO::getTagGroupId, tagGroupId)
        );
        return ResponseVO.success();
    }


    /**
     * ??????????????????
     *
     * @param updateNameVO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO updateTagGroupName(SysTagGroupUpdateNameVO updateNameVO, TagBrandsInfoVO brandsInfo) {
        //????????????????????????
        updateNameVO.setTagGroupName(updateNameVO.getTagGroupName().trim());

        //????????????????????????
        if (getByTagGroupNameCount(updateNameVO.getTagGroupName(), updateNameVO.getTagGroupId(), brandsInfo) > 0) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NAME_EXISTS.getMessage());
        }

        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        sysTagGroupDO.setTagGroupName(updateNameVO.getTagGroupName());
        sysTagGroupDO.setId(updateNameVO.getTagGroupId());
        sysTagGroupDO.setOrgId(brandsInfo.getOrgId());
        sysTagGroupDO.setBrandsId(brandsInfo.getBrandsId());
        int updateCount = sysTagGroupMapper.updateById(sysTagGroupDO);
        if (updateCount > 0) {
            return ResponseVO.success();
        }
        return ResponseVO.errorParams("????????????");
    }

    public ResponseVO downLoadMemberPackage(String json, TagBrandsInfoVO brandsInfoVO) {
        try {

            //  ????????????
            //    SysDictVO sysDictVOParam = new SysDictVO();
            //    sysDictVOParam.setDictParentId(excelModel);
            //   ResponseVO<List<SysDictVO>> dictResponse = dataManagerService.findAll(sysDictVOParam);
            //??????????????????
           /* ExcelUtil.noModelWrite();
            if(0 == dictResponse.getCode()){
                List<SysDictVO> list = dictResponse.getData();
                List<SysDictVO> newList = list.stream().sorted(Comparator.comparing(SysDictVO::getOrderNumber)).collect(Collectors.toList());
                List<String> columnList = new ArrayList<String>();
                List<List<String>> headList = new ArrayList<List<String>>();
                for(SysDictVO sysDictVO : newList){
                    List<String> head = new ArrayList<String>();
                    head.add(sysDictVO.getDictName());
                    headList.add(head);
                    columnList.add(sysDictVO.getDictValue());
                }*/
            JSONObject excelObj = JSON.parseObject(json);
            Object tagGroupIdObj = excelObj.get("tagGroupId");
            if (null == tagGroupIdObj) {
                return ResponseVO.errorParams("??????id????????????");
            }
            //todo ???????????????????????? ?? ????????????
            Long tagGroupId = Long.parseLong(tagGroupIdObj.toString());

            //????????????excel?????????
            Map<String, List> map = tagGroupUserService.getUsersDetailToExecl(excelObj, BusinessEnum.TAG_GROUP_EXCEL_EXPORT.getCode(), brandsInfoVO);

            //???????????????
            List<List<String>> headList = (List<List<String>>) map.get("head");

            //????????????
            List<List<Object>> dataList = (List<List<Object>>) map.get("data");

            //???????????????????????????excel??????
            ExcelUtil.noModelWrite(headList, dataList);
            /*}else{
                return ResponseVO.error(ResponseEnum.FALL_BACK_INFO.getCode(), "??????????????????");
            }*/
            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.FALL_BACK_INFO);
        }
    }


    /**
     * ??????????????????
     *
     * @param tagGroupName
     * @return
     */
    protected Integer getByTagGroupNameCount(String tagGroupName, Long id, TagBrandsInfoVO brandsInfo) {
        SysTagGroupDO sysTagGroupDO = new SysTagGroupDO();
        sysTagGroupDO.setTagGroupName(tagGroupName);
        sysTagGroupDO.setBrandsId(brandsInfo.getBrandsId());
        sysTagGroupDO.setOrgId(brandsInfo.getOrgId());
        sysTagGroupDO.setDeleted(BusinessEnum.NOTDELETED.getCode());

        if (null == id) {
            return sysTagGroupMapper.selectCount(new LambdaQueryWrapper<>(sysTagGroupDO));
        } else {
            //?????????????????????  ??????????????????????????????
            return sysTagGroupMapper.selectCount(new LambdaQueryWrapper<>(sysTagGroupDO).ne(SysTagGroupDO::getId, id));
        }
    }

    /**
     * ??????????????????????????????
     * true ?????????
     * false ?????????
     *
     * @param groupId
     * @return
     */
    protected boolean checkModifyDepending(Long groupId) {
        // TODO: 2020/9/16 ??????????????????  ???????????????????????????  
        // TODO: 2020/9/24 ?????????????????? 
        return false;
    }

    /**
     * ??????????????????????????????
     * true ?????????
     * false ?????????
     *
     * @param tagGroupId
     * @return
     */
    protected boolean checkDeleteDepending(Long tagGroupId) {
        //  2020/9/24 ????????????????????????
        Integer count = sysTagGroupUserMapper.selectCount(new LambdaQueryWrapper<SysTagGroupUserDO>()
                .eq(SysTagGroupUserDO::getTagGroupId, tagGroupId)
                .eq(SysTagGroupUserDO::getDeleted, BusinessEnum.NOTDELETED.getCode()));

        return count > 0;
    }

    /**
     * @Description: ????????????id??????????????????
     **/
    public void deleteByPropertyId(Long groupId) {
        SysTagGroupRuleDO sysTagGroupRuleDO = new SysTagGroupRuleDO();
        sysTagGroupRuleDO.setDeleted(BusinessEnum.DELETED.getCode());
        sysTagGroupRuleMapper.update(sysTagGroupRuleDO,
                new LambdaUpdateWrapper<SysTagGroupRuleDO>()
                        .eq(SysTagGroupRuleDO::getTagGroupId, groupId)
        );
    }

    /**
     * ????????????id ????????????????????????????????????
     *
     * @param tagGroupId
     * @return
     */
    public ResponseVO<SysTagGroupResponseVO> getGroupById(Long tagGroupId, TagBrandsInfoVO brandsInfo) {
        SysTagGroupDO sysTagGroupDO = getTagGroup(tagGroupId, brandsInfo);
        if (null == sysTagGroupDO) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }
        SysTagGroupResponseVO data = new SysTagGroupResponseVO();
        BeanUtils.copyPropertiesIgnoreNull(sysTagGroupDO, data);
        return ResponseVO.success(data);
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
     * ????????????id ????????????????????????
     *
     * @param tagGroupId
     * @return
     */
    public ResponseVO<SysTagGroupRuleGetResponseVO> getGroupRuleById(Long tagGroupId, TagBrandsInfoVO brandsInfo) {

        SysTagGroupDO sysTagGroupDO = getTagGroup(tagGroupId, brandsInfo);
        if (null == sysTagGroupDO) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }

        SysTagGroupRuleGetResponseVO data = new SysTagGroupRuleGetResponseVO();
//        //????????????
        BeanUtils.copyPropertiesIgnoreNull(sysTagGroupDO, data);
        //????????????
        data.setRule(tagGroupRuleService.getRule(tagGroupId));
        return ResponseVO.success(data);
    }

    /**
     * ???????????????code???????????????code
     *
     * @param dictParentCode
     * @return
     */
    private HashMap<Long, String> getDictCodeMap(String dictParentCode) {
        HashMap<Long, String> resultMap = new HashMap<>();

        DictFindAllVO dictFindAllVO = new DictFindAllVO();
        dictFindAllVO.setDictParentCode(dictParentCode);
        ResponseVO<List<SysDictVO>> dictResponseVO = dataManagerService.findAll(dictFindAllVO);
        if (CollUtil.isNotEmpty(dictResponseVO.getData())) {
            for (SysDictVO dictVO : dictResponseVO.getData()) {
                resultMap.put(dictVO.getId(), dictVO.getDictCode());
            }
        }
        return resultMap;
    }

    private List<SysTagGroupRuleDO> getRuleList(Long groupId, Long parentId) {
        LambdaQueryWrapper<SysTagGroupRuleDO> query = new LambdaQueryWrapper<>();
        query.eq(SysTagGroupRuleDO::getTagGroupId, groupId)
                .eq(SysTagGroupRuleDO::getTagGroupRuleParentId, parentId)
                .eq(SysTagGroupRuleDO::getDeleted, BusinessEnum.NOTDELETED.getCode());
        return sysTagGroupRuleMapper.selectList(query);
    }

    /**
     * ????????????????????????
     *
     * @param tag
     * @param logicalOperationDictCode
     * @return
     */
    private Set<Long> getTagUser(SysTagGroupRuleInfoRequestVO tag, String logicalOperationDictCode) {

        List<LogicalOperationValueRequestVO> tagPropertyList = tag.getLogicalOperationValue();
        List<Long> propertyIds = tagPropertyList.stream().map(LogicalOperationValueRequestVO::getTagPropertyId).collect(Collectors.toList());

        QueryWrapper<SysTagPropertyUserDO> query = new QueryWrapper();
        query.eq("tag_id", tag.getTagId());

        if (StringUtils.equals(TagDictEnum.GROUP_OPERATORS_IN.getCode(), logicalOperationDictCode)) {
            query.in("property_id", propertyIds);
        } else if (StringUtils.equals(TagDictEnum.GROUP_OPERATORS_NOT_IN.getCode(), logicalOperationDictCode)) {
            query.notIn("property_id", propertyIds);
        } else {
            //???????????? todo
        }
        List<SysTagPropertyUserDO> list = sysTagPropertyUserMapper.selectList(query);
        return list.stream().map(userDO -> userDO.getUserId()).filter(x -> x != null).collect(Collectors.toSet());
    }


    /**
     * ??????????????????
     *
     * @param first
     * @param second
     * @param relationCode
     * @return
     */
    private Set<Long> dealListByRelationCode(Set<Long> first, Set<Long> second, String relationCode) {
        if (StringUtils.equals(TagDictEnum.TAG_RELATIONAL_OPERATION_AND.getCode(), relationCode)) {
            //??????
            first.retainAll(second);
        } else if (StringUtils.equals(TagDictEnum.TAG_RELATIONAL_OPERATION_OR.getCode(), relationCode)) {
            //??????
            first.removeAll(second);
            first.addAll(second);
        }
        return first;
    }

    /**
     * ????????????????????????
     *
     * @param tagGroupDO
     * @param userIdSet
     * @return
     */
    private Set<Long> dealUserList(SysTagGroupDO tagGroupDO, Set<Long> userIdSet) {
        List<Long> userIdList = new ArrayList<>();
        userIdList.addAll(userIdSet);

        //??????null??????
        userIdList.remove(null);

        Set<Long> userListResult = new HashSet<>();

        //?????????
        if (userIdList.size() > 0) {
            // ???????????????????????????
            //????????????id?????????
            if (StringUtils.isNotBlank(tagGroupDO.getExcludeUserGroupId())) {
                //?????????????????????userList
                List<Long> excludeUserList = getExcludeUserList(tagGroupDO.getExcludeUserGroupId());
                if (CollUtil.isNotEmpty(excludeUserList)) {
                    userIdList.removeAll(excludeUserList);
                }
            }

            //????????????
            Integer limit = getLimit(userIdList.size(), tagGroupDO);
            if (null == limit) {
                //?????????
                userListResult.addAll(userIdList);
            } else if (limit < 1) {
            } else {

                //?????????
                while (userIdList.size() > 0 && userListResult.size() < limit) {
                    int index = new Random().nextInt(userIdList.size());

                    userListResult.add(userIdList.get(index));
                    userIdList.remove(index);
                }
            }

        }

        return userListResult;
    }


    private List<Long> getExcludeUserList(String excludeUserGroupId) {
        List<Long> excludeGroupList = Arrays.asList(excludeUserGroupId.split(",")).stream().map(s -> Long.parseLong(s)).collect(Collectors.toList());
        List<SysTagGroupUserDO> excludeUserDOList = sysTagGroupUserMapper.selectList(new LambdaQueryWrapper<SysTagGroupUserDO>()
                .in(SysTagGroupUserDO::getTagGroupId, excludeGroupList)
                .eq(SysTagGroupUserDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
        );
        if (CollUtil.isNotEmpty(excludeGroupList)) {
            return excludeUserDOList.stream().map(SysTagGroupUserDO::getUserId).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param total
     * @param tagGroupDO
     * @return null ??????????????????????????????????????????????????????
     */
    private Integer getLimit(Integer total, SysTagGroupDO tagGroupDO) {
        Integer limit = null;
        if (total > 0) {
            //???????????????
            if (null != tagGroupDO.getTagGroupCountLimitPercent()) {
                limit = Integer.valueOf((new BigDecimal(total).multiply(new BigDecimal(tagGroupDO.getTagGroupCountLimitPercent())).divide(new BigDecimal(100)).setScale(0, BigDecimal.ROUND_UP)).toString());
            }
            //???????????????
            if (null != tagGroupDO.getTagGroupCountLimitNum()) {
                limit = tagGroupDO.getTagGroupCountLimitNum();
            }
        }
        return limit;
    }

    /**
     * @param sysTagGroupRuleVO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO editGroupRule(SysTagGroupRuleVO sysTagGroupRuleVO, TagBrandsInfoVO brandsInfo) {
        // TODO: 2020/9/24 ??????????????????????????????????????????????????????

        //????????????  ??????????????????????????????????????????
        if (null != sysTagGroupRuleVO.getTagGroupCountLimitNum() && null != sysTagGroupRuleVO.getTagGroupCountLimitPercent()) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_USER_CONTROL_TOTAL_ERROR.getMessage());
        }

        Long tagGroupId = sysTagGroupRuleVO.getId();
        //????????????????????????
        SysTagGroupDO tagGroupDOFind = getTagGroup(tagGroupId, brandsInfo);
        if (null == tagGroupDOFind) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }

        //  ?????????????????????????????????
        if (checkTagStatus(sysTagGroupRuleVO.getRule())) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_TAG_NOT_USE.getMessage());
        }

        //??????????????????
        if (checkTagProperty(sysTagGroupRuleVO.getRule())) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_TAG_NOT_USE.getMessage());
        }

        //????????????????????????
        //?????? ??????????????????????????????????????????????????????????????????????????????updateWrapper ????????????
        this.update(new SysTagGroupDO(),new LambdaUpdateWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getId, tagGroupId)
                .set(SysTagGroupDO::getTagGroupRuleRelationshipId, sysTagGroupRuleVO.getTagGroupRuleRelationshipId())
                .set(SysTagGroupDO::getTagGroupCountLimitNum, sysTagGroupRuleVO.getTagGroupCountLimitNum())
                .set(SysTagGroupDO::getTagGroupCountLimitPercent, sysTagGroupRuleVO.getTagGroupCountLimitPercent())
                .set(SysTagGroupDO::getExcludeUserGroupId, sysTagGroupRuleVO.getExcludeUserGroupId())
                .set(SysTagGroupDO::getTagGroupRuleChangeExecuteStatus, BusinessEnum.UNEXECUTED.getCode())
        );


        // ??????????????????????????????
        if (getRuleCount(tagGroupId) > 0) {
            deleteByPropertyId(tagGroupId);
        }

//        tagGroupDOFind.setTagGroupCountLimitPercent(sysTagGroupRuleVO.getTagGroupCountLimitPercent());
//        tagGroupDOFind.setTagGroupCountLimitNum(sysTagGroupRuleVO.getTagGroupCountLimitNum());
//        tagGroupDOFind.setExcludeUserGroupId(sysTagGroupRuleVO.getExcludeUserGroupId());
//        tagGroupDOFind.setTagGroupRuleRelationshipId(sysTagGroupRuleVO.getTagGroupRuleRelationshipId());
        //????????????
        return saveGroupRule(tagGroupDOFind, sysTagGroupRuleVO.getRule(), true);

    }

    /**
     * ??????????????????
     *
     * @param ruleList
     * @return
     */
    private boolean checkTagStatus(List<SysTagGroupRuleRequestVO> ruleList) {
        HashSet<Long> tagSet = new HashSet<>();
        for (SysTagGroupRuleRequestVO group : ruleList) {
            tagSet.addAll(group.getTagGroupRule().stream()
                    .map(SysTagGroupRuleInfoRequestVO::getTagId).collect(Collectors.toList()));
        }

        Integer tagCount = sysTagMapper.selectCount(new LambdaQueryWrapper<SysTagDO>()
                .in(SysTagDO::getId, tagSet)
                .eq(SysTagDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .eq(SysTagDO::getTagStatus, BusinessEnum.USING.getCode())
        );
        return tagSet.size() != tagCount;
    }

    /**
     * ????????????????????????
     *
     * @param ruleList
     * @return
     */
    private boolean checkTagProperty(List<SysTagGroupRuleRequestVO> ruleList) {
        Set<Long> tagPropertySet;
        List<SysTagGroupRuleInfoRequestVO> tagList;
        for (SysTagGroupRuleRequestVO group : ruleList) {
            tagList = group.getTagGroupRule();

            for (SysTagGroupRuleInfoRequestVO tag : tagList) {
                tagPropertySet = tag.getLogicalOperationValue().stream().map(LogicalOperationValueRequestVO::getTagPropertyId).collect(Collectors.toSet());
                Integer propertyCount = sysTagPropertyMapper.selectCount(new LambdaQueryWrapper<SysTagPropertyDO>()
                        .in(SysTagPropertyDO::getId, tagPropertySet)
                        .eq(SysTagPropertyDO::getTagId, tag.getTagId())
                        .eq(SysTagPropertyDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                );
                return propertyCount != tagPropertySet.size();
            }
        }

        return false;
    }


    /**
     * ??????????????????
     *
     * @param sysTagGroupDO ????????????
     * @param tagGroupRule  ????????????
     * @param calculate     ????????????
     * @return
     */
    protected ResponseVO saveGroupRule(SysTagGroupDO sysTagGroupDO, List<SysTagGroupRuleRequestVO> tagGroupRule, boolean calculate) {
        SysTagGroupRuleDO parentRuleDO;
        SysTagGroupRuleDO ruleDO;
        for (SysTagGroupRuleRequestVO ruleAddVO : tagGroupRule) {
            // ??????
            parentRuleDO = new SysTagGroupRuleDO();
            parentRuleDO.setTagGroupId(sysTagGroupDO.getId());
            parentRuleDO.setTagGroupRuleParentId(0L);
            parentRuleDO.setTagGroupRuleRelationshipId(ruleAddVO.getTagGroupRuleRelationshipId());
            sysTagGroupRuleMapper.insert(parentRuleDO);
            //????????????
            List<SysTagGroupRuleDO> ruleDOList = new ArrayList<>();
            for (SysTagGroupRuleInfoRequestVO ruleInfoAddVO : ruleAddVO.getTagGroupRule()) {
                ruleDO = new SysTagGroupRuleDO();
                ruleDO.setTagGroupId(sysTagGroupDO.getId());
                ruleDO.setTagGroupRuleParentId(parentRuleDO.getId());
                ruleDO.setPropertyIds(this.getPropIds(ruleInfoAddVO));
                ruleDO.setTagGroupRule(JSON.toJSONString(ruleInfoAddVO));
                ruleDO.setTagId(ruleInfoAddVO.getTagId());
                ruleDOList.add(ruleDO);
            }
            tagGroupRuleService.saveBatch(ruleDOList);
        }

        // TODO: 2020/9/17 ????????????    ???????????????????????????????????????
        //????????????????????? ?????????
        if (calculate
                && BusinessEnum.USING.getCode().equals(sysTagGroupDO.getTagGroupStatus())
//                && BusinessEnum.TAG_GROUP_TYPE_STATIC.getCode().equals(sysTagGroupDO.getTagGroupStartType())
                && BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode().equals(sysTagGroupDO.getTagGroupStartType())
        ) {
            //??????????????????set ????????????
            SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
            countUserInfo.dealCountUserInfo();
            SysTagGroupDO groupDO = new SysTagGroupDO();
            groupDO.setId(sysTagGroupDO.getId());
            groupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));
            sysTagGroupMapper.updateById(groupDO);
            producerService.sendMessage(tagGroupUserTopic, String.valueOf(sysTagGroupDO.getId()), randomUtil.getRandomIndex());
        }
        return ResponseVO.success(sysTagGroupDO.getId());
    }

    /**
     * ????????????propIds
     *
     * @param vo
     * @return
     */
    private String getPropIds(SysTagGroupRuleInfoRequestVO vo) {
        List<LogicalOperationValueRequestVO> logicalOperationValue = vo.getLogicalOperationValue();
        String ids = "";
        for (LogicalOperationValueRequestVO logicalOperationValueRequestVO : logicalOperationValue) {
            ids = ids + logicalOperationValueRequestVO.getTagPropertyId() + ",";
        }
        ids = ids.substring(0, ids.lastIndexOf(","));
        return ids;
    }


    /**
     * ???????????????????????????????????????
     *
     * @return
     */
    public List<SysTagGroupDO> getTagGroupListByDataManager(String brandAndOrgId) {
        if (StringUtils.isBlank(brandAndOrgId)) {
            return new ArrayList<>();
        }

        String[] ids = brandAndOrgId.split(",");
        Long brandsId = Long.valueOf(ids[0]);
        Long orgId = Long.valueOf(ids[1]);
        //????????????????????????
        List<SysTagGroupDO> groupDOList = sysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .eq(SysTagGroupDO::getTagGroupStatus, BusinessEnum.USING.getCode())
                .eq(SysTagGroupDO::getBrandsId, brandsId)
                .eq(SysTagGroupDO::getOrgId, orgId)
                .eq(SysTagGroupDO::getTagGroupStartType, BusinessEnum.TAG_GROUP_START_ASSIGN_DATE.getCode())
                .le(SysTagGroupDO::getTagGroupStartTime, new Date())
        );

        //??????????????????????????????
        groupDOList.addAll(sysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .eq(SysTagGroupDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .eq(SysTagGroupDO::getTagGroupStatus, BusinessEnum.USING.getCode())
                .eq(SysTagGroupDO::getBrandsId, brandsId)
                .eq(SysTagGroupDO::getOrgId, orgId)
                .eq(SysTagGroupDO::getTagGroupStartType, BusinessEnum.TAG_GROUP_START_IMMEDIATELY.getCode())
                .eq(SysTagGroupDO::getTagGroupType, BusinessEnum.TAG_GROUP_TYPE_DYNAMIC.getCode())
        ));

        //??????????????????  ??????redis ????????? databaseId_tagGroup_static_ids
        Iterator<SysTagGroupDO> it = groupDOList.iterator();
        SysTagGroupDO sysTagGroupDO;
        while (it.hasNext()) {
            sysTagGroupDO = it.next();
            //?????????????????????
            if (getRuleCount(sysTagGroupDO.getId()) < 1) {
                it.remove();
                continue;
            }

            //??????????????? ????????????????????????
            if (BusinessEnum.TAG_GROUP_TYPE_STATIC.getCode().equals(sysTagGroupDO.getTagGroupType())) {
                if (redisUtil.sHasKey(TagConstant.GROUP_STATIC_EXECUTED_REDIS_KEY_PREFIX + brandsId, sysTagGroupDO.getId())) {
                    it.remove();
                }
            }
        }
        return groupDOList;
    }

    /**
     * @param tagGroupDO
     * @return null ????????????
     */
    public Set<Long> getUserListByGroupId(SysTagGroupDO tagGroupDO) {
        //????????????list
        Set<Long> userIdList = new HashSet<>();
        Long tagGroupId = tagGroupDO.getId();

        //????????????id ????????????code
        HashMap<Long, String> relationshipMap = getDictCodeMap(TagDictEnum.TAG_RELATIONAL_OPERATION.getCode());
        HashMap<Long, String> logicalOperationMap = getDictCodeMap(TagDictEnum.GROUP_OPERATORS.getCode());
        if (relationshipMap.keySet().size() < 1 || logicalOperationMap.keySet().size() < 1) {
            return null;
        }
        //???????????? ???/???
        String groupRelationCode = relationshipMap.get(tagGroupDO.getTagGroupRuleRelationshipId());
        if (StrUtil.isBlank(groupRelationCode)) {
            return null;
        }
        //?????????
        List<SysTagGroupRuleDO> parentRuleList = getRuleList(tagGroupId, 0L);
        //?????????
        List<SysTagGroupRuleDO> childRuleDOList;
        //????????????????????????list
        Set<Long> groupInnerUserIdList;

        int groupCount = 0;
        for (SysTagGroupRuleDO parentRuleDO : parentRuleList) {
            //??????????????? ??? ??????????????? ???????????????????????? ????????????????????????
            if (groupCount > 1 && StringUtils.equals(TagDictEnum.TAG_RELATIONAL_OPERATION_AND.getCode(), groupRelationCode) && CollUtil.isEmpty(userIdList)) {
                break;
            }

            //???????????????/???code
            String groupInnerRelationshipCode = relationshipMap.get(parentRuleDO.getTagGroupRuleRelationshipId());
            if (StrUtil.isBlank(groupRelationCode)) {
                return null;
            }

            //???????????????
            childRuleDOList = getRuleList(tagGroupId, parentRuleDO.getId());

            groupInnerUserIdList = this.getUserIdListByRuleDOList(childRuleDOList, groupInnerRelationshipCode, logicalOperationMap);

            //?????????????????????/???
            if (groupCount == 0) {
                userIdList = groupInnerUserIdList;
            } else {
                dealListByRelationCode(userIdList, groupInnerUserIdList, groupRelationCode);
            }
            groupCount++;
        }

        //????????????????????????
        return dealUserList(tagGroupDO, userIdList);
    }

    private Set<Long> getUserIdListByRuleDOList(List<SysTagGroupRuleDO> childRuleDOList, String relationCode, HashMap<Long, String> logicalOperationMap) {
        List<SysTagGroupRuleInfoRequestVO> ruleList = childRuleDOList.stream().map(rule -> JSON.parseObject(rule.getTagGroupRule(), SysTagGroupRuleInfoRequestVO.class)).collect(Collectors.toList());
        return this.getUserIdListByRuleVOList(ruleList, relationCode, logicalOperationMap);
    }

    private Set<Long> getUserIdListByRuleVOList(List<SysTagGroupRuleInfoRequestVO> ruleList, String relationCode, HashMap<Long, String> logicalOperationMap) {
        Set<Long> groupInnerUserIdList = null;

        int count = 0;
        for (SysTagGroupRuleInfoRequestVO tag : ruleList) {
            if (count > 1 && StringUtils.equals(TagDictEnum.TAG_RELATIONAL_OPERATION_AND.getCode(), relationCode) && CollUtil.isEmpty(groupInnerUserIdList)) {
                break;
            }

            Set<Long> tagUserList = getTagUser(tag, logicalOperationMap.get(tag.getLogicalOperationId()));
            //?????????????????????/???
            if (0 == count) {
                groupInnerUserIdList = tagUserList;
            } else {
                dealListByRelationCode(groupInnerUserIdList, tagUserList, relationCode);
            }

            count++;
        }
        if (null == groupInnerUserIdList) {
            return new HashSet<>();
        }
        return groupInnerUserIdList;
    }
}
