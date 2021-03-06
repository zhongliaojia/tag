package org.etocrm.tagManager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.etocrm.core.enums.BusinessEnum;
import org.etocrm.core.enums.ResponseEnum;
import org.etocrm.core.util.JsonUtil;
import org.etocrm.core.util.ResponseVO;
import org.etocrm.dynamicDataSource.service.IDynamicService;
import org.etocrm.dynamicDataSource.util.BasePage;
import org.etocrm.dynamicDataSource.util.RedisUtil;
import org.etocrm.tagManager.batch.impl.common.BatchLifeCycleCommonService;
import org.etocrm.tagManager.constant.LifeCycleConstant;
import org.etocrm.tagManager.constant.TagConstant;
import org.etocrm.tagManager.enums.TagErrorMsgEnum;
import org.etocrm.tagManager.mapper.ISysTagGroupMapper;
import org.etocrm.tagManager.mapper.ISysTagGroupUserMapper;
import org.etocrm.tagManager.mapper.ISysTagMapper;
import org.etocrm.tagManager.mapper.ISysTagPropertyMapper;
import org.etocrm.tagManager.mapper.mat.IMatMetadataWorkProcessHandleMapper;
import org.etocrm.tagManager.mapper.mat.IMatMetadataWorkProcessMapper;
import org.etocrm.tagManager.model.DO.*;
import org.etocrm.tagManager.model.DO.mat.MatWorkProcessDO;
import org.etocrm.tagManager.model.DO.mat.MatWorkProcessHandleDO;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessHandleVO;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessVO;
import org.etocrm.tagManager.model.VO.tag.TagBrandsInfoVO;
import org.etocrm.tagManager.model.VO.tagGroup.*;
import org.etocrm.tagManager.service.AsyncServiceManager;
import org.etocrm.tagManager.service.ITagGroupRuleService;
import org.etocrm.tagManager.service.ITagGroupUserService;
import org.etocrm.tagManager.service.mat.IMatAutomationMarketingService;
import org.etocrm.tagManager.util.BrandsInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RefreshScope
@Service
@Slf4j
public class TagGroupServiceUserImpl extends ServiceImpl<ISysTagGroupUserMapper, SysTagGroupUserDO> implements ITagGroupUserService {

    @Autowired
    IDynamicService dynamicService;

    @Autowired
    private AsyncServiceManager asyncServiceManager;

    @Autowired
    private ITagGroupRuleService tagGroupRuleService;

    @Autowired
    private ISysTagGroupUserMapper sysTagGroupUserMapper;

    @Autowired
    private ISysTagGroupMapper sysTagGroupMapper;

    @Autowired
    private ISysTagMapper sysTagMapper;

    @Autowired
    private ISysTagPropertyMapper sysTagPropertyMapper;

    @Autowired
    private BrandsInfoUtil brandsInfoUtil;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BatchLifeCycleCommonService lifeCycleService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IMatAutomationMarketingService iMatAutomationMarketingService;

    @Autowired
    private IMatMetadataWorkProcessMapper iMatMetadataWorkProcessMapper;

    @Autowired
    private IMatMetadataWorkProcessHandleMapper iMatMetadataWorkProcessHandleMapper;

//    @Value("${ETL.WRITER.MAX_NUMBER}")
//    private int WRITER_MAX_NUMBER;
//
//    @Value("${ETL.DELETE.NUMBER}")
//    private int DELETE_NUMBER;

    private static final int TAG = 0;
    private static final int LIFE_CYCLE = 1;

    /**
     * ??????????????????id????????????????????????
     *
     * @param pageVO
     * @return
     */
    @Override
    public ResponseVO getTagGroupUsersDetail(SysTagGroupUserDetailPageVO pageVO) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            return applicationContext.getBean(TagGroupServiceUserImpl.class).getTagGroupUsersDetailTransactional(pageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_GET_ERROR);
        }
    }

    //   @Transactional(rollbackFor = {Exception.class})
    public ResponseVO getTagGroupUsersDetailTransactional(SysTagGroupUserDetailPageVO pageVO) throws Exception {
        //   String queryWay = "";
        //  List<HashMap> data = new ArrayList<>();

        /* //??????tagGroupId??????????????????count??????????????????????????????
            QueryWrapper<SysTagGroupUserDetailSplitDO> query = new QueryWrapper();
            query.eq("tag_group_id",pageVO.getTagGroupId());
            Integer count = iSysTagGroupUserSplitMapper.selectCount(query);
            if(count == 0){
                queryWay = "noSplit";
            }*/


        BasePage data = getUserDetails(pageVO);

        return ResponseVO.success(data);
    }

    /**
     * ??????????????????count???????????????????????????????????????????????????
     *
     * @param tagGroupId
     * @return
     */
    @Override
    public ResponseVO getTagGroupUsersDetailCount(Long tagGroupId) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            return applicationContext.getBean(TagGroupServiceUserImpl.class).getTagGroupUsersDetailCountTransactional(tagGroupId, brandsInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_GET_ERROR);
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param groupUserPOList
     * @return
     */
    @Override
    @Async//@Async("asyncServiceExecutor")
    public ResponseVO asyncSaveBatchGroupUser(Long groupId, List<SysTagGroupUserPO> groupUserPOList) {
        if (CollectionUtil.isNotEmpty(groupUserPOList)) {
            //??????????????????
            int limit = countStep(groupUserPOList.size());
            List<List<SysTagGroupUserPO>> groupUserPOs = new ArrayList<>();
            Integer writeMaxNum = Integer.valueOf(redisUtil.getValueByKey(TagConstant.WRITER_MAX_NUMBER).toString());
            Stream.iterate(0, n -> n + 1).limit(limit).forEach(i -> {
                groupUserPOs.add(groupUserPOList.stream().skip(i * writeMaxNum).limit(writeMaxNum).collect(Collectors.toList()));
            });
            CountDownLatch countDownLatch = new CountDownLatch(groupUserPOs.size());
            for (List<SysTagGroupUserPO> groupUserPOSplit : groupUserPOs) {
                //??????????????????
                this.batchInsertTagGroupData(groupUserPOSplit, countDownLatch);
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                log.error("asyncSaveBatchGroupUser countDownLatch error", e);
                Thread.currentThread().interrupt();
            }

            //todo ??????????????? ????????????????????? ??????????????????????????????????????????
            SysTagGroupCountUserInfo countUserInfo = this.getCountUserInfo(groupId);
            SysTagGroupDO tagGroupDO = new SysTagGroupDO();
            tagGroupDO.setCountUserInfo(JSON.toJSONString(countUserInfo));
            tagGroupDO.setId(groupId);
            sysTagGroupMapper.updateById(tagGroupDO);

//            redisUtil.deleteCache("groupUserCount_" + tokenVO.getBrandsId()+"_"+ tokenVO.getOrgId() + "_" + groupId);
//            int count = sysTagGroupUserMapper.selectCount(new LambdaQueryWrapper<SysTagGroupUserDO>().eq(SysTagGroupUserDO::getTagGroupId, groupId));
//            log.info("+++++++++++++++++++++++count:{}", count);
        }
        return ResponseVO.success();
    }

    /**
     * ????????????????????????
     *
     * @param tagGroupIds
     * @return
     */
    @Override
    public ResponseVO<TagGroupTableResponseVO> getTableInfo(List<Long> tagGroupIds) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            if (CollectionUtil.isEmpty(tagGroupIds)) {
                return ResponseVO.errorParams("??????id????????????");
            }
            List<SysTagGroupDO> tagGroupDOList = this.getTagGroupByIds(tagGroupIds, brandsInfo);

            //????????????????????????
            if (tagGroupIds.size() != tagGroupDOList.size()) {
                return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
            }

            TagGroupTableResponseVO data = new TagGroupTableResponseVO();
            //????????????
            boolean queryRule = tagGroupIds.size() > 1;
            if (queryRule) {
                data.setGroupInfo(this.getGroupInfo(tagGroupDOList));
            }
            //????????????
            data.setTableData(this.getTableData(tagGroupDOList, brandsInfo));

            return ResponseVO.success(data);
        } catch (Exception e) {
            log.error("======== getTableInfo error,groupIds:{},e:{}", tagGroupIds, e.getMessage(), e);
        }
        return ResponseVO.errorParams("????????????");
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private List<TagGroupInfoResponseVO> getGroupInfo(List<SysTagGroupDO> tagGroupDOList) {
        List<TagGroupInfoResponseVO> groupInfoList = new ArrayList<>();
        TagGroupInfoResponseVO groupInfo;
        for (SysTagGroupDO tagGroup : tagGroupDOList) {
            groupInfo = new TagGroupInfoResponseVO();
            groupInfo.setTagGroupId(tagGroup.getId());
            groupInfo.setTagGroupName(tagGroup.getTagGroupName());
            groupInfo.setRelationshipId(tagGroup.getTagGroupRuleRelationshipId());
            groupInfo.setGroupRule(tagGroupRuleService.getRule(tagGroup.getId()));

            groupInfoList.add(groupInfo);
        }
        return groupInfoList;
    }

    /**
     * ????????????dashboard table ??????
     *
     * @param
     * @return
     */
    private List<TagGroupTableDataVO> getTableData(List<SysTagGroupDO> tagGroupDOList, TagBrandsInfoVO brandsInfo) {
        Long beginTime = System.currentTimeMillis();

        Map tableDataMap = this.getTableDataMap(tagGroupDOList);
        List<TagGroupTableDataVO> tableData = (List<TagGroupTableDataVO>) tableDataMap.get("tableData");
        List<Map> list = (List<Map>) tableDataMap.get("list");
        CompletableFuture[] cfs = list.stream()
                .map(data -> CompletableFuture
                        .supplyAsync(() ->
                                this.getTableNumber((Integer) data.get("colIndex"), (Long) data.get("tagGroupId"), brandsInfo)
                        ).whenComplete((result, th) -> {
                                    tableData.get((Integer) data.get("colIndex")).getValue().set((Integer) data.get("lineIndex"), result);
                                }
                        )
                )
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(cfs).join();

        log.info("======== ?????? tableInfo  cost???{}", System.currentTimeMillis() - beginTime);

        return tableData;
    }

    private Map getTableDataMap(List<SysTagGroupDO> tagGroupDOList) {
        List<TagGroupTableDataVO> tableData = new ArrayList<>();
        List<Map> list = new ArrayList<>();
        TagGroupTableDataVO tableDataVO;
        for (int i = 0; i < tagGroupDOList.size(); i++) {
            Long tagGroupId = tagGroupDOList.get(i).getId();
            for (int j = 0; j < TagConstant.TABLE_NAME.length; j++) {
                if (i == 0) {
                    tableDataVO = new TagGroupTableDataVO();
                    tableDataVO.setName(TagConstant.TABLE_NAME[j]);
                    tableDataVO.setValue(initArray(tagGroupDOList.size()));
                    tableData.add(j, tableDataVO);
                }
                Map map = new HashMap();
                map.put("tagGroupId", tagGroupId);
                map.put("lineIndex", i);
                map.put("colIndex", j);
                list.add(map);
            }
        }
        Map map = new HashMap();
        map.put("tableData", tableData);
        map.put("list", list);
        return map;
    }

    private List<String> initArray(int size) {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            strings.add("");
        }
        return strings;
    }

    private String getTableNumber(int index, Long tagGroupId, TagBrandsInfoVO brandsInfo) {
        String result = "";
        Long beginTime = System.currentTimeMillis();
        switch (index) {
            case 0:
                //??????
                result = String.valueOf(sysTagGroupUserMapper.selectCount(new LambdaQueryWrapper<SysTagGroupUserDO>().eq(SysTagGroupUserDO::getTagGroupId, tagGroupId)));
                log.info("======== ????????????sql cost???{}", System.currentTimeMillis() - beginTime);

                break;
            case 1:
                //?????????
                //select count(1) from sys_tag_group_user t1,sys_tag_property_user t2
                //where t1.user_id = t2.user_id and t1.tag_group_id=? and t2.tag_id=?

                //select count(1) from sys_tag_group_user where tag_group_id=? and exists (select 1 from sys_tag_property_user where tag_id=? and user_id= sys_tag_group_user.user_id)
                String existsSql = "select 1 from members where id=sys_tag_group_user.user_id and org_id=#{orgId} and brands_id=#{brandsId} and vip_level_id is not null"
                        .replace("#{orgId}", String.valueOf(brandsInfo.getOrgId()))
                        .replace("#{brandsId}", String.valueOf(brandsInfo.getBrandsId()));
                result = String.valueOf(sysTagGroupUserMapper.selectCount(new LambdaQueryWrapper<SysTagGroupUserDO>()
                        .eq(SysTagGroupUserDO::getTagGroupId, tagGroupId)
                        .exists(existsSql)
                ));
                log.info("======== ???????????????sql cost???{}", System.currentTimeMillis() - beginTime);
                break;
            case 2:
                //????????????
                // TODO: 2020/11/30 ??????????????????????????? ???????????????????????????
                existsSql = "select 1 from members where id=sys_tag_group_user.user_id and org_id=#{orgId} and brands_id=#{brandsId} and first_consume_time is not null"
                        .replace("#{orgId}", String.valueOf(brandsInfo.getOrgId()))
                        .replace("#{brandsId}", String.valueOf(brandsInfo.getBrandsId()));
                result = String.valueOf(sysTagGroupUserMapper.selectCount(new LambdaQueryWrapper<SysTagGroupUserDO>()
                        .eq(SysTagGroupUserDO::getTagGroupId, tagGroupId)
                        .exists(existsSql)
                ));
                log.info("======== ??????????????????sql cost???{}", System.currentTimeMillis() - beginTime);
                break;
            case 3:
                //?????????????????????
                // TODO: 2020/11/20 ??????etl????????????
                //recent_year_consume_amount
                // select sum(recent_year_consume_amount) from members where
                // org_id=1 and brands_id=2 and exists (select 1 from sys_tag_group_user where user_id=members.id and tag_group_id=1)
                List<String> tableNames = Arrays.asList(new String[]{"members"});
                String column = "sum(recent_year_consume_amount)";
                String whereClause = "org_id=#{orgId} and brands_id=#{brandsId} and exists (select 1 from sys_tag_group_user where user_id=members.id and tag_group_id=#{tagGroupId} and is_delete=0)"
                        .replace("#{orgId}", String.valueOf(brandsInfo.getOrgId()))
                        .replace("#{brandsId}", String.valueOf(brandsInfo.getBrandsId()))
                        .replace("#{tagGroupId}", String.valueOf(tagGroupId));
                result = dynamicService.getString(tableNames, column, whereClause);
                log.info("======== ???????????????????????????sql cost???{}", System.currentTimeMillis() - beginTime);
                break;
            case 4:
                //????????????????????????
                // TODO: 2020/11/20 ??????????????????
                //recent_year_consume_times
                // select sum(recent_year_consume_times) from members where org_id=1 and brands_id=2
                // and exists (select 1 from sys_tag_group_user where user_id=members.id and tag_group_id=1)
                tableNames = Arrays.asList(new String[]{"members"});
                column = "sum(recent_year_order_count)";
                whereClause = "org_id=#{orgId} and brands_id=#{brandsId} and exists (select 1 from sys_tag_group_user where user_id=members.id and tag_group_id=#{tagGroupId} and is_delete=0)"
                        .replace("#{orgId}", String.valueOf(brandsInfo.getOrgId()))
                        .replace("#{brandsId}", String.valueOf(brandsInfo.getBrandsId()))
                        .replace("#{tagGroupId}", String.valueOf(tagGroupId))
                ;
                result = dynamicService.getString(tableNames, column, whereClause);
                log.info("======== ??????????????????????????????sql cost???{}", System.currentTimeMillis() - beginTime);
                break;
            default:
                break;
        }
        return null == result ? "" : result;
    }

    /**
     * ????????????ids ????????????list
     * ?????? order by id asc
     *
     * @param tagGroupIds
     * @return
     */
    private List<SysTagGroupDO> getTagGroupByIds(List<Long> tagGroupIds, TagBrandsInfoVO tagBrandsInfoVO) {
        return sysTagGroupMapper.selectList(new LambdaQueryWrapper<SysTagGroupDO>()
                .select(SysTagGroupDO::getId, SysTagGroupDO::getTagGroupName, SysTagGroupDO::getTagGroupRuleRelationshipId)
                .eq(SysTagGroupDO::getOrgId, tagBrandsInfoVO.getOrgId())
                .eq(SysTagGroupDO::getBrandsId, tagBrandsInfoVO.getBrandsId())
                .in(SysTagGroupDO::getId, tagGroupIds)
        );
    }


    /**
     * ????????????????????????
     *
     * @param requestVO
     * @return
     */
    @Override
    public ResponseVO<TagGroupChartResponseVO> getChartInfo(TagGroupChartRequestVO requestVO) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }

            List<SysTagGroupDO> tagGroupDOList = this.getTagGroupByIds(requestVO.getTagGroupIds(), brandsInfo);
            int size = requestVO.getTagGroupIds().size();
            //????????????????????????
            if (size != tagGroupDOList.size()) {
                return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
            }
            if (StringUtils.equals(requestVO.getTagCode(), LifeCycleConstant.TAG_CODE)) {
                return this.getLifeCycleData(brandsInfo, tagGroupDOList);
            } else {
                return this.getTagData(requestVO.getTagCode(), brandsInfo, tagGroupDOList);
            }

        } catch (Exception e) {
            log.error("======== getChartInfo error,requestVO:{},e:{}", requestVO, e.getMessage(), e);
        }
        return ResponseVO.errorParams("??????????????????");
    }

    @Override
    public ResponseVO<List<TagClassesInfoVO>> getTagClassesList() {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            List<TagClassesInfoVO> tagClassesInfoVOS = this.getTagClassesInfo(brandsInfo);
            if (null == tagClassesInfoVOS) {
                return ResponseVO.success(new ArrayList<>());
            }
            return ResponseVO.success(tagClassesInfoVOS);
        } catch (Exception e) {
            log.error("getTagClassesList error,e:{}", e.getMessage(), e);
        }
        return ResponseVO.errorParams("????????????");
    }

    private ResponseVO<TagGroupChartResponseVO> getLifeCycleData(TagBrandsInfoVO brandsInfo, List<SysTagGroupDO> tagGroupDOList) throws ExecutionException, InterruptedException {
        SysLifeCycleModelDO modelDO = lifeCycleService.getModelByBrandInfo(brandsInfo);
        if (null == modelDO) {
            return ResponseVO.success();
        }
        int oldData = 0;
        if (null == modelDO.getDataUpdateTime() || modelDO.getDataUpdateTime().before(modelDO.getRuleUpdateTime())){
            oldData = 1;
        }
        TagGroupChartResponseVO data = new TagGroupChartResponseVO();
        data.setTagCode(LifeCycleConstant.TAG_CODE);
        data.setTagName(modelDO.getName());
        data.setOldData(oldData);
        data.setDataInfo(this.getDataInfo(tagGroupDOList, modelDO.getId(), LIFE_CYCLE));

        return ResponseVO.success(data);
    }

    public static void main(String[] args) {
        System.out.println(new Date().before(DateUtil.tomorrow()));
        System.out.println(new Date().before(new Date()));
    }
    private List<TagGroupChartDataInfoVO> getDataInfo(List<SysTagGroupDO> tagGroupIdList, Long id, int type) throws ExecutionException, InterruptedException {
        List<TagGroupChartDataInfoVO> data = new ArrayList<>();
        List<SysLifeCycleModelRuleDO> modelRuleDOList = null;
        List<SysTagPropertyDO> propertyRuleDOList = null;

        if (LIFE_CYCLE == type) {
            modelRuleDOList = lifeCycleService.getRuleListByModelId(id);
        } else {
            propertyRuleDOList = this.getPropertyByTagId(id);
        }

        TagGroupChartDataInfoVO dataInfo;
        for (SysTagGroupDO tagGroupDO : tagGroupIdList) {
            dataInfo = new TagGroupChartDataInfoVO();
            dataInfo.setTagGroupId(tagGroupDO.getId());
            dataInfo.setTagGroupName(tagGroupDO.getTagGroupName());
            if (LIFE_CYCLE == type) {
                dataInfo.setTagData(this.getLifeCycleDataInfo(modelRuleDOList, tagGroupDO.getId()));
            } else {
                dataInfo.setTagData(this.getTagDataInfo(propertyRuleDOList, tagGroupDO.getId()));
            }

            data.add(dataInfo);
        }
        return data;
    }

    private List<TagGroupChartTagDataInfoVO> getLifeCycleDataInfo(List<SysLifeCycleModelRuleDO> ruleDOList, Long tagGroupId) throws ExecutionException, InterruptedException {
        TagGroupChartTagDataInfoVO[] dataArray = new TagGroupChartTagDataInfoVO[ruleDOList.size()];
        Long beginTime = System.currentTimeMillis();
        CompletableFuture[] cfArr = ruleDOList.stream().
                map(rule -> CompletableFuture
                        .supplyAsync(() -> getLifeCycleStepDataInfo(rule, tagGroupId))
                        .whenComplete((result, th) -> {
                            dataArray[ruleDOList.indexOf(rule)] = result;
                        })).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(cfArr).join();
        log.info("====== ????????????????????????????????????{}", System.currentTimeMillis() - beginTime);
        AtomicReference<Integer> count = new AtomicReference<>(0);
        Arrays.stream(dataArray).filter(tagData -> StrUtil.isNotBlank(tagData.getValue()))
                .forEach(tagData -> count.updateAndGet(v -> v + Integer.valueOf(tagData.getValue())));
        BigDecimal total = new BigDecimal(count.get());
        for (TagGroupChartTagDataInfoVO info : dataArray) {
            if (StrUtil.isBlank(info.getValue()) || "0".equals(info.getValue())) {
                info.setPercent("0%");
            } else {
                info.setPercent((new BigDecimal(info.getValue()).divide(total, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2)) + "%");
            }
        }
        return Arrays.asList(dataArray);
    }

    private TagGroupChartTagDataInfoVO getLifeCycleStepDataInfo(SysLifeCycleModelRuleDO modelRuleDO, Long tagGroupId) {
        TagGroupChartTagDataInfoVO dataInfo = new TagGroupChartTagDataInfoVO();
        dataInfo.setName(modelRuleDO.getStepName());
        Long stepBeginTime = System.currentTimeMillis();

        LambdaQueryWrapper<SysTagGroupUserDO> queryWrapper = new LambdaQueryWrapper<>();
        String existsSql = "select 1 from sys_life_cycle_model_user where model_rule_id=" + modelRuleDO.getId()
                + " and user_id= sys_tag_group_user.user_id and is_delete=0";
        queryWrapper.exists(existsSql);
        queryWrapper.eq(SysTagGroupUserDO::getTagGroupId, tagGroupId);
        Integer count = sysTagGroupUserMapper.selectCount(queryWrapper);
        log.info("====== ?????????????????????{} ?????????????????????{}", modelRuleDO.getStepName(), System.currentTimeMillis() - stepBeginTime);
        dataInfo.setValue(String.valueOf(count));

        return dataInfo;
    }

    private ResponseVO getTagData(String tagCode, TagBrandsInfoVO brandsInfo, List<SysTagGroupDO> tagGroupDOList) throws ExecutionException, InterruptedException {
        TagInfoVO tagInfoVO = this.getTagInfoVO(tagCode, brandsInfo);
        if (null == tagInfoVO || null == tagInfoVO.getId()) {
            return ResponseVO.success();
        }

        SysTagDO tagDO = this.getTagByTagId(tagInfoVO.getId(), brandsInfo);
        if (null == tagDO) {
            return ResponseVO.success();
        }
        int oldData = 0;
        if (BusinessEnum.UNEXECUTED.getCode().equals(tagDO.getTagPropertyChangeExecuteStatus())){
            oldData = 1;
        }
        TagGroupChartResponseVO data = new TagGroupChartResponseVO();
        data.setTagCode(tagInfoVO.getCode());
        data.setTagName(tagDO.getTagName());
        data.setTagId(tagDO.getId());
        data.setOldData(oldData);
        data.setDataInfo(this.getDataInfo(tagGroupDOList, tagDO.getId(), TAG));

        return ResponseVO.success(data);
    }

    private TagInfoVO getTagInfoVO(String tagCode, TagBrandsInfoVO brandsInfo) {

        List<TagClassesInfoVO> tagClassesInfoVOS = this.getTagClassesInfo(brandsInfo);
        if (null == tagClassesInfoVOS) {
            return null;
        }
        List<TagInfoVO> tagInfoList = tagClassesInfoVOS.stream().flatMap(tagClassesInfo -> tagClassesInfo.getTagList().stream())
                .filter(tag -> StringUtils.equals(tagCode, tag.getCode())).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(tagInfoList)) {
            return null;
        }
        return tagInfoList.get(0);
    }

    private List<TagClassesInfoVO> getTagClassesInfo(TagBrandsInfoVO brandsInfo) {
        Object dashboardStrObj = redisUtil.getValueByKey(TagConstant.TAG_GROUP_DASHBOARD_PREFIX + brandsInfo.getBrandsId());
        if (null != dashboardStrObj) {
            return JSON.parseArray(dashboardStrObj.toString(), TagClassesInfoVO.class);
        }
        return null;
//        JSONObject dashboardTagInfo = JSONObject.parseObject(dashboardTagInfoStr);
//        if (null == dashboardTagInfo) {
//            return null;
//        }
//        String dataKey = brandsInfo.getOrgId() + "_" + brandsInfo.getBrandsId();
//        JSONArray tagInfo = dashboardTagInfo.getJSONArray(dataKey);
//        if (CollectionUtil.isEmpty(tagInfo)) {
//            return null;
//        }
//        return JSONArray.parseArray(JSON.toJSONString(tagInfo), TagClassesInfoVO.class);
    }

    /**
     * ????????????????????????
     *
     * @param tagPropertyDOList
     * @param tagGroupId
     * @return
     */
    private List<TagGroupChartTagDataInfoVO> getTagDataInfo(List<SysTagPropertyDO> tagPropertyDOList, Long tagGroupId) {
        List<TagGroupChartTagDataInfoVO> data = new ArrayList<>();

        List<String> tableName = new ArrayList<>();
        tableName.add("sys_tag_property_user t1");
        tableName.add("sys_tag_group_user t2");
        Long beginTime = System.currentTimeMillis();

        //todo ????????????????????????add array ?????????
//        CompletableFuture[] cfArr = tagPropertyDOList.stream().
//                map(propertyDO -> CompletableFuture
//                        .supplyAsync(() -> getPropertyDataInfo(propertyDO, tagGroupId,tableName))
//                        .whenComplete((result, th) -> {
//                            data.add(result);
//                        })).toArray(CompletableFuture[]::new);
//        CompletableFuture.allOf(cfArr).join();
        Integer count = 0;
        TagGroupChartTagDataInfoVO propertyDataInfo;
        for (SysTagPropertyDO propertyDO : tagPropertyDOList) {
            propertyDataInfo = getPropertyDataInfo(propertyDO, tagGroupId, tableName);
            data.add(propertyDataInfo);

            if (StrUtil.isNotBlank(propertyDataInfo.getValue())) {
                count += Integer.valueOf(propertyDataInfo.getValue());
            }
        }
        BigDecimal total = new BigDecimal(count);
        for (TagGroupChartTagDataInfoVO info : data) {
            if (StrUtil.isBlank(info.getValue()) || "0".equals(info.getValue())) {
                info.setPercent("0%");
            } else {
                info.setPercent(new BigDecimal(info.getValue()).divide(total, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2) + "%");
            }
        }
        log.info("====== ??????????????????????????????{}", System.currentTimeMillis() - beginTime);
        return data;
    }

    private TagGroupChartTagDataInfoVO getPropertyDataInfo(SysTagPropertyDO tagPropertyDO, Long tagGroupId, List<String> tableName) {
        TagGroupChartTagDataInfoVO dataInfo = new TagGroupChartTagDataInfoVO();
        dataInfo.setName(tagPropertyDO.getPropertyName());
        Long propertyBeginTime = System.currentTimeMillis();
        // ?????????????????????????????????
        // select count(1) from sys_tag_property_user t1,sys_tag_group_user t2
        // where t1.user_id=t2.user_id and t1.property_id=? and t2.tag_group_id=?
        String whereClause = "t1.user_id=t2.user_id and t1.is_delete=0 and t2.is_delete=0 and t1.property_id=" + tagPropertyDO.getId() + " and t2.tag_group_id=" + tagGroupId;
        int count = dynamicService.count(tableName, whereClause);
        log.info("====== ???????????????{} ?????????????????????{}", tagPropertyDO.getPropertyName(), System.currentTimeMillis() - propertyBeginTime);

        dataInfo.setValue(String.valueOf(count));
        return dataInfo;
    }

    private List<SysTagPropertyDO> getPropertyByTagId(Long tagId) {
        return sysTagPropertyMapper.selectList(new LambdaQueryWrapper<SysTagPropertyDO>().eq(SysTagPropertyDO::getTagId, tagId));
    }

    private SysTagDO getTagByTagId(Long tagId, TagBrandsInfoVO brandsInfoVO) {
        return sysTagMapper.selectOne(new LambdaQueryWrapper<SysTagDO>()
                .select(SysTagDO::getId, SysTagDO::getTagName,SysTagDO::getTagPropertyChangeExecuteStatus)
                .eq(SysTagDO::getOrgId, brandsInfoVO.getOrgId())
                .eq(SysTagDO::getBrandsId, brandsInfoVO.getBrandsId())
                .eq(SysTagDO::getId, tagId)
        );
    }

    /**
     * ????????????id????????????count
     *
     * @param groupId
     * @return
     */
    private SysTagGroupCountUserInfo getCountUserInfo(Long groupId) {
        SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
        countUserInfo.zeroCountUserInfo();
        //??????sql??????????????????
        List<String> tableNames = new ArrayList<String>();
        tableNames.add("sys_tag_group_user t1");
        tableNames.add("members t2");
        List<String> columns = new ArrayList<>();
        columns.add("COUNT(t2.id) countUser");
        columns.add("COUNT(DISTINCT t2.unionid) countUnionId");
        columns.add("COUNT(DISTINCT t2.mobile) countMobileId");
        columns.add("COUNT(DISTINCT t2.number) countMemberId");
        columns.add("count(t1.is_delete) deleted ");

        String whereClause = "t1.user_id = t2.id AND t1.tag_group_id = " + groupId;
        Map countMap = dynamicService.selectById(tableNames, columns, whereClause);
        for (Object key : countMap.keySet()) {
            String keyStr = String.valueOf(key);
            switch (keyStr) {
                case "countUser":
                    countUserInfo.setCountUser(String.valueOf(countMap.get(key)));
                    break;
                case "countUnionId":
                    countUserInfo.setCountUnionID(String.valueOf(countMap.get(key)));
                    break;
                case "countMobileId":
                    countUserInfo.setCountMobileId(String.valueOf(countMap.get(key)));
                    break;
                case "countMemberId":
                    countUserInfo.setCountMemberId(String.valueOf(countMap.get(key)));
                    break;
                default:
                    break;
            }
        }
        return countUserInfo;
    }

    private void batchInsertTagGroupData(List<SysTagGroupUserPO> groupUserPOList, CountDownLatch countDownLatch) {
        List<HashMap<String, Object>> hashMaps = JsonUtil.JsonToMapList(JsonUtil.toJson(groupUserPOList));
        Set<String> strings = hashMaps.get(0).keySet();
        String column = humpToLine2(StringUtils.join(strings, ","));
        List<String> columns = Arrays.asList(column.split(","));
        TableName table = SysTagGroupUserPO.class.getAnnotation(TableName.class);
        String tableName = "";
        if (table != null) {
            tableName = table.value();
        }
        dynamicService.insertPlusRecord(tableName, columns, hashMaps, countDownLatch);
    }

    //  @Transactional(rollbackFor = {Exception.class})
    public ResponseVO getTagGroupUsersDetailCountTransactional(Long tagGroupId, TagBrandsInfoVO brandsInfoVO) {
        //????????????????????????
        SysTagGroupDO tagGroup = getTagGroup(tagGroupId, brandsInfoVO);
        if (null == tagGroup) {
            return ResponseVO.errorParams(TagErrorMsgEnum.TAG_GROUP_NOT_EXISTS.getMessage());
        }
        QueryWrapper query = new QueryWrapper();
        query.eq("tag_group_id", tagGroupId);
        Integer count = sysTagGroupUserMapper.selectCount(query);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("count", count);
        return ResponseVO.success(jsonObject);
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
     * ??????????????????id??????????????????????????????????????????????????????????????????
     *
     * @param splitVO
     * @return
     */
    @Override
    public ResponseVO splitTagGroupUsersDetail(SysTagGroupUserSplitDetailVO splitVO) {
        try {
            TagBrandsInfoVO brandsInfo = brandsInfoUtil.getBrandsInfo();
            if (null != brandsInfo.getResponseVO()) {
                return brandsInfo.getResponseVO();
            }
            return applicationContext.getBean(TagGroupServiceUserImpl.class).splitTagGroupUsersDetailTransactional(splitVO/*, dataSourceId*/);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(4001, "??????????????????");
        }
    }

    public ResponseVO splitTagGroupUsersDetailTransactional(SysTagGroupUserSplitDetailVO splitVO/*, Long dataSourceId*/) throws Exception {

        log.info("----------------??????????????????-----------------");

        asyncServiceManager.asyncSplitTagGroupUsersDetail(splitVO/*, dataSourceId*/);

        return ResponseVO.success("???????????????????????????");
    }


    /**
     * ??????????????????????????????Execl??????
     * * @return
     */
    @Override
    public Map<String, List> getUsersDetailToExecl(JSONObject excelObj, Integer exportType, TagBrandsInfoVO brandsInfoVO) {

        //json?????????????????????????????????????????????tagGroupId/tagId,key?????????????????????value?????????????????????????????????value

        String encryption = String.valueOf(excelObj.get("encryption"));
        Map<String, List> map = new HashMap<>();
        try {
            //??????tagGroupId????????????
            if (BusinessEnum.TAG_GROUP_EXCEL_EXPORT.getCode().equals(exportType)) {
                map = getGroupUserDetailsToExcel(excelObj);
            } else {
                map = getTagUserDetailsToExcel(excelObj, brandsInfoVO);
            }

            List<TreeMap<String, Object>> userDetailsList = map.get("data");

            if (CollUtil.isEmpty(userDetailsList)) {
                return map;
            }

            List<List<String>> dataList = new ArrayList<>();
            for (TreeMap<String, Object> stringObjectHashMap : userDetailsList) {
                List<String> colmnsList = new LinkedList<>();
                List<ExportOrderVO> exportorderList = new ArrayList();
                Iterator ketIterator = stringObjectHashMap.keySet().iterator();
                while (ketIterator.hasNext()) {
                    String key = String.valueOf(ketIterator.next());
                    String value = String.valueOf(stringObjectHashMap.get(key));

                    ExportOrderVO exportOrderVO = new ExportOrderVO();
                    if (key.contains("gender")) {
                        if (value.equals("1")) {
                            exportOrderVO = exportOrder(exportOrderVO, key, "???");
                        } else if (value.equals("2")) {
                            exportOrderVO = exportOrder(exportOrderVO, key, "???");
                        } else {
                            exportOrderVO = exportOrder(exportOrderVO, key, "??????");
                        }
                        exportorderList.add(exportOrderVO);
                    } else if (key.contains("mobile") /*&& encryption.equals("true")*/) {
                        if (value.length() > 10) {
                            StringBuilder sb = new StringBuilder(value);
                            String mobile = sb.replace(3, 7, "****").toString();
                            exportOrderVO = exportOrder(exportOrderVO, key, mobile);
                            exportorderList.add(exportOrderVO);
                        }
                    } else {
                        exportOrderVO = exportOrder(exportOrderVO, key, value);
                        exportorderList.add(exportOrderVO);
                    }
                }
                List<ExportOrderVO> newExportorderList = exportorderList.stream().sorted(Comparator.comparing(ExportOrderVO::getOrder)).collect(Collectors.toList());
                for (ExportOrderVO exportOrderVO : newExportorderList) {
                    colmnsList.add(exportOrderVO.getColumn());
                }
                dataList.add(colmnsList);
            }
            //???????????????????????????data??????????????????????????????
            map.put("data", dataList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }

    /**
     * ????????????????????????
     *
     * ??????????????????????????????
     */
    @Override
    @Async
    public void asyncSaveBatchGroupUser(GroupUserSaveBatchVO groupUserSaveBatchVO) {
        try {
            this.saveBatchGroupUser(groupUserSaveBatchVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.error("??????????????????????????????,tagGroupId:{}",groupUserSaveBatchVO.getGroupId());
        }
    }

    /**
     * ???????????????
     * 0. ??????set ?????????
     * 1.???????????????????????????
     * 2.??????????????????
     * 3.?????????????????????0
     *
     * @param groupUserSaveBatchVO
     * @return
     */
    public ResponseVO saveBatchGroupUser(GroupUserSaveBatchVO groupUserSaveBatchVO) {
        Set<Long> userIdSet = groupUserSaveBatchVO.getUserIdList();
        Long tagGroupId = groupUserSaveBatchVO.getGroupId();

        log.info("?????????tagGroupId???" + tagGroupId);

        //????????????????????????
        String tableName = "sys_tag_group_user";
        String whereClause = " tag_group_id = " + tagGroupId + " limit " + Integer.valueOf(redisUtil.getValueByKey(TagConstant.DELETE_NUMBER).toString());
        boolean deleteFlag = true;
        while (deleteFlag) {
            int deleteCount = dynamicService.deleteRecord(tableName, whereClause);
            deleteFlag = deleteCount > 0;
        }

        SysTagGroupCountUserInfo countUserInfo = new SysTagGroupCountUserInfo();
        countUserInfo.zeroCountUserInfo();
        if (CollUtil.isNotEmpty(userIdSet)) {
            log.info("?????????tagGroupId:" + tagGroupId + " ?????????" + userIdSet.size());
            // ??????id ??????
            List<Long> userIdList = new ArrayList<>(userIdSet);
            Collections.sort(userIdList);

            //??????????????????
            int limit = countStep(userIdList.size());
            List<List<Long>> userIdSpiltList = new ArrayList<>();
            Integer writeMaxNum = Integer.valueOf(redisUtil.getValueByKey(TagConstant.WRITER_MAX_NUMBER).toString());
            Stream.iterate(0, n -> n + 1).limit(limit).forEach(i -> {
                userIdSpiltList.add(userIdList.stream().skip(i * writeMaxNum).limit(writeMaxNum).collect(Collectors.toList()));
            });
            CountDownLatch countDownLatch = new CountDownLatch(userIdSpiltList.size());
            for (List<Long> userIdSpilt : userIdSpiltList) {
                //??????????????????
                applicationContext.getBean(TagGroupServiceUserImpl.class).asyncInstallTagGroupData(tagGroupId, userIdSpilt, countDownLatch);
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                log.error("saveBatchGroupUserTransactional countDownLatch.await() error", e);
                Thread.currentThread().interrupt();
            }
            countUserInfo = this.getCountUserInfo(tagGroupId);
        }

        log.info("???????????????0");
        sysTagGroupMapper.update(new SysTagGroupDO(), new LambdaUpdateWrapper<SysTagGroupDO>()
                //???????????????0
                .set(SysTagGroupDO::getTagGroupSplitCount, 0L)
                //???????????????0 ?????????????????????
                .set(SysTagGroupDO::getSonCountInfo, null)
                .set(SysTagGroupDO::getCountUserInfo, JSON.toJSONString(countUserInfo))
                //set ???????????????
                .set(SysTagGroupDO::getTagGroupRuleChangeExecuteStatus,BusinessEnum.EXECUTED.getCode())
                .eq(SysTagGroupDO::getId, tagGroupId));

        //???????????????????????????????????????????????????????????????????????????
        RecalculationTagGroupUserByWorkRule(tagGroupId);
        log.info( "???????????????????????????????????????tagGroupId=???"+tagGroupId+"???");
        return ResponseVO.success();
    }


    private void RecalculationTagGroupUserByWorkRule(Long tagGroupId){
        log.info( "???????????????????????????????????????tagGroupId=???"+tagGroupId+"???");
        try {
            QueryWrapper<MatWorkProcessDO> queryWorks = new QueryWrapper<>();
            queryWorks.eq("user_group_id",tagGroupId);
            queryWorks.eq("is_delete",BusinessEnum.NOTDELETED.getCode());
            List<MatWorkProcessDO> matWorkProcessDOS = iMatMetadataWorkProcessMapper.selectList(queryWorks);
            for(MatWorkProcessDO processDO : matWorkProcessDOS){
                if(processDO.getType()==1 || !processDO.getTriggerCondition().equals("{}")){//????????????????????????
                    continue;
                }
                MatWorkProcessVO  processVO = new MatWorkProcessVO();
                QueryWrapper<MatWorkProcessHandleDO> queryHandles = new QueryWrapper<>();
                queryHandles.eq("work_id",processDO.getId());
                queryHandles.eq("is_delete",BusinessEnum.NOTDELETED.getCode());
                List<MatWorkProcessHandleDO> matWorkProcessHandleDOS = iMatMetadataWorkProcessHandleMapper.selectList(queryHandles);
                List<MatWorkProcessHandleVO> handleVOS = new ArrayList<>();
                Map<Long, Long> handleIdMap = new HashMap<>();
                for(MatWorkProcessHandleDO handleDO : matWorkProcessHandleDOS){
                    handleIdMap.put(handleDO.getMatId(),handleDO.getId());
                    MatWorkProcessHandleVO handleVO = new MatWorkProcessHandleVO();
                    handleVO.setId(handleDO.getMatId());
                    handleVO.setPercent(handleDO.getPercent());
                }
                processVO.setHandleId(processVO.getHandleId());
                processVO.setHandles(handleVOS);
                processVO.setHandleIdMap(handleIdMap);
                processVO.setId(processDO.getMatId());
                processVO.setHandleType(processDO.getHandleType());
                processVO.setOrgId(processDO.getOrgId());
                processVO.setWorksId(processDO.getId());
                processVO.setIsOpenAbtest(processDO.getIsOpenAbtest());
                processVO.setUserGroupId(tagGroupId);
                if(processDO.getConditions() != null){
                    processVO.setSearchCondition(JSON.parseObject(processDO.getConditions()));
                }

                iMatAutomationMarketingService.calculationTagGroupByProcessRule(processVO);
            }
        }catch (Exception e){
            log.info( "?????????????????????????????????????????????tagGroupId=???"+tagGroupId+"???");
            log.error(e.getMessage(), e);
        }

    }


    /**
     * ??????????????????
     */
    private Integer countStep(Integer size) {
        Integer writeMaxNum = Integer.valueOf(redisUtil.getValueByKey(TagConstant.WRITER_MAX_NUMBER).toString());
        return (size + writeMaxNum - 1) / writeMaxNum;
    }

    /**
     * @param userIdList
     */
    @Async//@Async("asyncServiceExecutor")
    public void asyncInstallTagGroupData(Long groupId, List<Long> userIdList, CountDownLatch countDownLatch) {
        // ??????userId ???????????????????????????
        List<SysTagGroupUserPO> groupUserPOList = new ArrayList<>(userIdList.size());
        SysTagGroupUserPO groupUserPO;
        for (Long userId : userIdList) {
            groupUserPO = new SysTagGroupUserPO();
            groupUserPO.setTagGroupId(groupId);
            groupUserPO.setUserId(userId);
            groupUserPOList.add(groupUserPO);
        }

        List<HashMap<String, Object>> hashMaps = JsonUtil.JsonToMapList(JsonUtil.toJson(groupUserPOList));
        Set<String> strings = hashMaps.get(0).keySet();
        String column = humpToLine2(StringUtils.join(strings, ","));
        List<String> columns = Arrays.asList(column.split(","));
        TableName table = SysTagGroupUserPO.class.getAnnotation(TableName.class);
        String tableName = "";
        if (table != null) {
            tableName = table.value();
        }
        dynamicService.insertPlusRecord(tableName, columns, hashMaps, countDownLatch);
    }

    public static String humpToLine2(String str) {
        Pattern humpPattern = Pattern.compile("[A-Z]");
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    private ExportOrderVO exportOrder(ExportOrderVO exportOrderVO, String key, String value) throws Exception {

        switch (key) {
            case "subcontract_no":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(0);
                break;
            case "number":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(1);
                break;
            case "name":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(2);
                break;
            case "gender":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(3);
                break;
            case "birthday":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(4);
                break;
            case "vip_level":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(5);
                break;
            case "integral":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(6);
                break;
            case "registered_time":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(7);
                break;
            case "mobile":
                exportOrderVO.setColumn(value);
                exportOrderVO.setTitle(key);
                exportOrderVO.setOrder(8);
                break;
            default:
                break;
        }

        return exportOrderVO;
    }

    private Map<String, List> getGroupUserDetailsToExcel(JSONObject jsonOb) throws Exception {

        String orderBy = " sys_tag_group_user.subcontract_no asc ";
        List<String> tableNames = new ArrayList<String>();

        tableNames.add("sys_tag_group_user");
        tableNames.add("members");
        String whereClause = "sys_tag_group_user.user_id = members.id AND sys_tag_group_user.tag_group_id = " + jsonOb.get("tagGroupId");

        Map<String, List> map = propertyToList(jsonOb);

        //??????sql??????????????????
        List<String> columns = (List<String>) map.get("columns");

        List<TreeMap> mapList = dynamicService.selectList(tableNames, columns, whereClause, orderBy);

        map.put("data", mapList);

        return map;
    }

    private Map<String, List> getTagUserDetailsToExcel(JSONObject jsonOb, TagBrandsInfoVO brandsInfoVO) throws Exception {

        List<String> tableNames = new ArrayList<String>();

        // select * from members  where exists (select 1 from sys_tag_property_user where tag_id=1 and user_id= members.id)
        tableNames.add("members ");
        String whereClause = " exists (select 1  from sys_tag_property_user where tag_id=" + jsonOb.get("tagId")
                + " and user_id = members.id)  AND members.brands_id=" + brandsInfoVO.getBrandsId()
                + " and members.org_id=" + brandsInfoVO.getOrgId();
        Map<String, List> map = propertyToList(jsonOb);

        //??????sql??????????????????
        List<String> columns = (List<String>) map.get("columns");

        List<TreeMap> mapList = dynamicService.selectList(tableNames, columns, whereClause, null);

        map.put("data", mapList);

        return map;
    }

    private Map<String, List> propertyToList(JSONObject jsonOb) throws Exception {

        Map<String, List> map = new HashMap();
        List<String> columns = new ArrayList<String>();
        List<List<String>> headList = new ArrayList<>();

        Set keySet = jsonOb.keySet();

        Iterator ketIterator = keySet.iterator();
        List<ExportOrderVO> exportorderList = new ArrayList();
        while (ketIterator.hasNext()) {
            String key = String.valueOf(ketIterator.next());
            String value = String.valueOf(jsonOb.get(key));

            ExportOrderVO exportOrderVO = new ExportOrderVO();
            exportOrderVO = exportOrder(exportOrderVO, value, key);
            if (exportOrderVO.getOrder() != null) {
                exportorderList.add(exportOrderVO);
            }
        }
        List<ExportOrderVO> newExportorderList = exportorderList.stream().sorted(Comparator.comparing(ExportOrderVO::getOrder)).collect(Collectors.toList());

        for (ExportOrderVO exportOrderVO : newExportorderList) {
            String title = exportOrderVO.getColumn();
            String column = exportOrderVO.getTitle();
            if (title != null && column != null && !column.equals("")) {
                List<String> titleList = new LinkedList<>();
                if (title.contains("?????????")) {
                    columns.add("sys_tag_group_user.subcontract_no subcontract_no");
                    titleList.add(title);
                    headList.add(titleList);
                } else {
                    columns.add("members." + column + " " + column);
                    titleList.add(title);
                    headList.add(titleList);
                }
            }
        }

        map.put("head", headList);
        map.put("columns", columns);
        return map;

    }

    private BasePage getUserDetails(SysTagGroupUserDetailPageVO pageVO) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        List<String> columns = new ArrayList<String>();
        String orderBy = "sys_tag_group_user.subcontract_no asc";
        //??????????????????????????????????????????????????????
        tableNames.add("sys_tag_group_user");
        tableNames.add("members");

        String whereClause = "sys_tag_group_user.user_id = members.id AND sys_tag_group_user.tag_group_id = " + pageVO.getTagGroupId();

        columns.add("sys_tag_group_user.subcontract_no subcontractNo");
        columns.add("members.number number");
        columns.add("members.name name");
        columns.add("members.gender gender");
        columns.add("members.birthday birthday");
        columns.add("members.vip_level vipLevel");
        columns.add("members.integral integral");
        columns.add("members.registered_time registeredTime");
        columns.add("members.mobile mobile");

        //    List<HashMap> mapList = dynamicService.selectList(tableNames, columns, whereClause,null);
        IPage page = dynamicService.selectListByPage(pageVO.getSize(), pageVO.getCurrent(), tableNames, columns, whereClause, orderBy);

        List<HashMap> records = page.getRecords();

        for (HashMap map : records) {
            String sex = String.valueOf(map.get("gender"));
            String subcontractNo = String.valueOf(map.get("subcontractNo"));
            String mobile = String.valueOf(map.get("mobile"));
            if (sex.equals("1")) {
                map.put("gender", "???");
            } else if (sex.equals("2")) {
                map.put("gender", "???");
            } else {
                map.put("gender", "??????");
            }
            if (subcontractNo.equals("ZZZ")) {
                map.put("subcontractNo", "");
            }
            if (mobile.length() > 10) {
                StringBuilder sb = new StringBuilder(mobile);
                mobile = sb.replace(3, 7, "****").toString();
            }
            map.put("mobile", mobile);

        }
        //   return mapList;
        page.setRecords(records);
        return new BasePage(page);
    }


    private void tagCodeInfo() {
        String tagCodeStr = "[{\"name\":\"??????\",\"code\":\"gender\"},{\"name\":\"??????\",\"code\":\"age\"},{\"name\":\"????????????\",\"code\":\"register_source\"},{\"name\":\"??????????????????\",\"code\":\"vip_level\"},{\"name\":\"???????????????\",\"code\":\"customer_unit _price\"},{\"name\":\"???????????????\",\"code\":\"order_count\"},{\"name\":\"???????????????\",\"code\":\"unit_price\"},{\"name\":\"??????????????????\",\"code\":\"pay_amount\"},{\"name\":\"????????????\",\"code\":\"bug_channel\"},{\"name\":\"????????????\",\"code\":\"bug_product_category\"},{\"name\":\"????????????\",\"code\":\"lify_cycle\"}]";

    }
}
