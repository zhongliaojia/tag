package org.etocrm.tagManager.service.impl.mat;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.etocrm.core.enums.BusinessEnum;
import org.etocrm.core.util.BeanUtils;
import org.etocrm.core.util.JsonUtil;
import org.etocrm.core.util.ResponseVO;
import org.etocrm.dynamicDataSource.service.IDynamicService;
import org.etocrm.dynamicDataSource.util.RedisConfig;
import org.etocrm.dynamicDataSource.util.RedisUtil;
import org.etocrm.tagManager.batch.impl.common.MatCommonService;
import org.etocrm.tagManager.mapper.mat.IMatCalculationDataMapper;
import org.etocrm.tagManager.mapper.mat.IMatMetadataWorkProcessActionMapper;
import org.etocrm.tagManager.mapper.mat.IMatMetadataWorkProcessHandleMapper;
import org.etocrm.tagManager.mapper.mat.IMatMetadataWorkProcessMapper;
import org.etocrm.tagManager.model.DO.mat.*;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessActionVO;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessHandleVO;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessSendRecordVO;
import org.etocrm.tagManager.model.VO.mat.MatWorkProcessVO;
import org.etocrm.tagManager.service.mat.IMatAutomationMarketingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
public class MatAutomationMarketingServiceImpl extends ServiceImpl<IMatCalculationDataMapper, MatCalculationDataDO> implements IMatAutomationMarketingService {


    @Autowired
    private IDynamicService iDynamicService;

    @Autowired
    private MatCommonService matCommonService;

    @Autowired
    private IMatMetadataWorkProcessMapper iMatMetadataWorkProcessMapper;

    @Autowired
    private IMatMetadataWorkProcessHandleMapper iMatMetadataWorkProcessHandleMapper;

    @Autowired
    private IMatMetadataWorkProcessActionMapper iMatMetadataWorkProcessActionMapper;

    @Autowired
    private RedisUtil redisUtil;

    //??????mat????????????Token?????? URL
    @Value("${mat.url.auth}")
    private String matAuthUrl;

    //??????????????????????????????mat?????? URL
    @Value("${mat.url.work}")
    private String matWorkNoticeUrl;

    @Value("${mat.url.grant_type}")
    private String grantType;

    @Value("${mat.url.client_id}")
    private String clientId;

    @Value("${mat.url.client_secret}")
    private String clientSecret;

    private static final String MAT_TOKEN_CACHE_KEY = "matTokenCacheKey";

    /**
     * ???????????????????????????????????????
     *
     * @param matWorkProcessVO
     * @return
     */
    @Override
    @Async
    public void calculationTagGroupByProcessRule(MatWorkProcessVO matWorkProcessVO) {

        /*if (matWorkProcessVO.getSearchCondition() == null || matWorkProcessVO.getSearchCondition().toJSONString().trim().equals("{}")) {
            return ResponseVO.error(4001, "????????????????????????");
        }*/
        ResponseVO responseVO = new ResponseVO(1001, "????????????");
        try {
            //????????????????????????
            String tableNames = "mat_map_calculation_data cd";
            String whereClause = " cd.mat_work_id =" + matWorkProcessVO.getId();
            iDynamicService.deleteRecord(tableNames, whereClause);

            if (matWorkProcessVO.getHandleType() == 1) {//???????????????
                responseVO = nothandleScreenOperator(matWorkProcessVO);//???????????????????????????????????????????????????
                noticeMatSystem(responseVO, matWorkProcessVO.getOrgId(), matWorkProcessVO.getId());//???????????????????????????mat??????

            } else if (matWorkProcessVO.getHandleType() == 2) {//?????????????????????????????????????????????
                responseVO = commonTypeMarketingRuleOperater(matWorkProcessVO);
                noticeMatSystem(responseVO, matWorkProcessVO.getOrgId(), matWorkProcessVO.getId());//???????????????????????????mat??????

            } else {
                ResponseVO.error(4001, "????????????????????????????????????");
            }
            log.info("????????????????????????responseVO =???" + responseVO.toString() + "???");

            //???????????????
        //    executeSubProcess(matWorkProcessVO.getSubProcessVO());

        } catch (Exception e) {
            if(matWorkProcessVO.getType() == null){
                log.info( "?????????????????????????????????????????????tagGroupId=???"+matWorkProcessVO.getUserGroupId()+"???,matWorkId=???"+matWorkProcessVO.getId()+"???");
            }
            log.error(e.getMessage(), e);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void saveMarketingRule(MatWorkProcessVO matWorkProcessVO){
        //?????????????????????????????????
        MatWorkProcessDO processDO = new MatWorkProcessDO();
        BeanUtils.copyPropertiesIgnoreNull(matWorkProcessVO, processDO);
        processDO.setMatId(matWorkProcessVO.getId());
        JSONObject searchCondition = matWorkProcessVO.getSearchCondition();
        JSONObject execConfig = matWorkProcessVO.getExecConfig();
        JSONObject sendLimitConfig = matWorkProcessVO.getSendLimitConfig();
        JSONObject triggerCondition = matWorkProcessVO.getTriggerCondition();

        processDO.setConditions(searchCondition == null ? null : searchCondition.toJSONString());
        processDO.setExecConfig(execConfig == null ? null : execConfig.toJSONString());
        processDO.setSendLimitConfig(sendLimitConfig == null ? null : sendLimitConfig.toJSONString());
        processDO.setTriggerCondition(triggerCondition == null ? null : triggerCondition.toJSONString());
        if (matWorkProcessVO.getBeginTime() == null || matWorkProcessVO.getBeginTime().trim().equals("")) {
            processDO.setBeginTime(null);
        }
        if (matWorkProcessVO.getEndTime() == null || matWorkProcessVO.getEndTime().trim().equals("")) {
            processDO.setEndTime(null);
        }
        if (matWorkProcessVO.getExecTime() == null || matWorkProcessVO.getExecTime().trim().equals("")) {
            processDO.setExecTime(null);
        }
        if (matWorkProcessVO.getLastExecTime() == null || matWorkProcessVO.getLastExecTime().trim().equals("")) {
            processDO.setLastExecTime(null);
        }

        matWorkProcessVO.getHandleId();

        iMatMetadataWorkProcessMapper.insert(processDO);
        matWorkProcessVO.setWorksId(processDO.getId());//??????????????????id??????
        Map<Long, Long> handleIdMap = new HashMap<>();
        Map<Long, Long> idMap = new HashMap<>();

        List<MatWorkProcessHandleVO> handles = matWorkProcessVO.getHandles();
        for (MatWorkProcessHandleVO handleVO : handles) {
            MatWorkProcessHandleDO handleDO = new MatWorkProcessHandleDO();
            BeanUtils.copyPropertiesIgnoreNull(handleVO, handleDO);
            if (handleVO.getExecTime().trim().equals("")) {
                handleDO.setExecTime(null);
            }
            handleDO.setWorkId(processDO.getId());
            handleDO.setMatId(handleVO.getId());
            handleDO.setMatWorkId(handleVO.getWorkId());
            iMatMetadataWorkProcessHandleMapper.insert(handleDO);
            handleIdMap.put(handleVO.getId(), handleDO.getId());
            idMap.put(handleVO.getId(), handleDO.getId());
        }
        matWorkProcessVO.setHandleIdMap(handleIdMap);
        List<MatWorkProcessActionVO> actions = matWorkProcessVO.getActions();
        for (MatWorkProcessActionVO actionVO : actions) {
            MatWorkProcessActionDO actionDO = new MatWorkProcessActionDO();
            BeanUtils.copyPropertiesIgnoreNull(actionVO, actionDO);
            actionDO.setMatId(actionVO.getId());
            actionDO.setMatWorkId(actionVO.getWorksId());
            actionDO.setMatHandleId(actionVO.getHandleId());
            actionDO.setWorksId(processDO.getId());
            actionDO.setDetails(actionVO.getDetails() == null ? null : actionVO.getDetails().toJSONString());
            Set<Long> longs = idMap.keySet();
            for (Long id : longs) {
                if (actionVO.getHandleId().equals(id)) {
                    actionDO.setHandleId(idMap.get(id));
                }
            }
            iMatMetadataWorkProcessActionMapper.insert(actionDO);
        }
    }



    //???????????????????????????mat??????
    void noticeMatSystem(ResponseVO responseVO, Long orgId, Long workId) {
        if (responseVO.getCode() == 0) {//code=0?????????????????????????????????????????????mat??????
            log.info("??????workId???" + workId + "???????????????????????????????????????????????????mat???????????????orgId=???" + orgId + "???");
            String token = this.getMatTokenByOrgId(String.valueOf(orgId));//????????????id??????token

            String workResult = "";
            JSONObject obj = new JSONObject();
            obj.put("organization_id", orgId);
            obj.put("matWorkId", workId);
            if (!token.equals("")) {

                log.info("------------------------------?????????workResult--obj------------------------------");
                log.info("????????????obj =???" + obj.toJSONString() + "???");

                workResult = HttpRequest.post(matWorkNoticeUrl)
                        .header(Header.CONTENT_TYPE, "application/json")
                        .header(Header.AUTHORIZATION, "Bearer " + token)
                        .body(obj.toJSONString())
                        .timeout(20000)//???????????????
                        .execute().body();
                log.info("????????????workResult =???" + workResult + "???");
            }
            //???????????????token??????
            if(workResult.contains("401") && workResult.contains("Unauthenticated")){
                redisUtil.deleteCache(MAT_TOKEN_CACHE_KEY + "_" + orgId);
                token = this.getMatTokenByOrgId(orgId.toString());
                workResult = HttpRequest.post(matWorkNoticeUrl)
                        .header(Header.CONTENT_TYPE, "application/json")
                        .header(Header.AUTHORIZATION, "Bearer " + token)
                        .body(obj.toJSONString())
                        .timeout(20000)//???????????????
                        .execute().body();
                log.info("???token?????????noticeMatSystem?????????????????????workResult =???" + workResult + "???");
            }
            log.info("------------------------------?????????workResult------------------------------");
        }
    }

    public String getMatTokenByOrgId(String orgId) {
        String token = redisUtil.getRefresh(MAT_TOKEN_CACHE_KEY + "_" + orgId, String.class);
        if (token == null || token.equals("")) {
            JSONObject obj = new JSONObject();
            obj.put("organization_id", orgId);
            obj.put("grant_type", grantType);
            obj.put("client_id", clientId);
            obj.put("client_secret", clientSecret);

            String result1 = HttpRequest.post(matAuthUrl)//????????????mat????????????token
                    .header(Header.CONTENT_TYPE, "application/json")
                    .body(obj.toJSONString())
                    .timeout(20000)//???????????????
                    .execute().body();

            log.info("------------------------------?????????result1------------------------------");
            log.info("????????????result1 =???" + result1 + "???");
            JSONObject tokenObj = JSON.parseObject(result1);
            token = tokenObj.get("access_token") == null ? "" : tokenObj.get("access_token").toString();
            if (!token.equals("")) {
                redisUtil.set(MAT_TOKEN_CACHE_KEY + "_" + orgId, token, RedisConfig.expire);
            }
        }else{
            log.info("????????????token =???" + token + "???");
        }
        return token;
    }

    /**
     * ??????????????????????????????
     *
     * @param matWorkProcessVO
     * @return
     */
    @Override
    public List<TreeMap> eventProcessPropertyScreen(MatWorkProcessVO matWorkProcessVO)  {
        List<TreeMap> treeMaps = matCommonService.changeRuleToSQLWithResult(matWorkProcessVO);
        return treeMaps;
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param sendRecordVOs
     * @return
     */
    @Override
    @Async
    public void asyncBatchSaveSendRecord(List<MatWorkProcessSendRecordVO> sendRecordVOs) {
        try {
            //CountDownLatch countDownLatch = new CountDownLatch(sendRecordVOs.size());
            /*List<HashMap<String, Object>> hashMaps = JsonUtil.JsonToMapList(JsonUtil.toJson(sendRecordVOs));
            Set<String> strings = hashMaps.get(0).keySet();
            String column = humpToLine2(StringUtils.join(strings, ","));
            List<String> columns = Arrays.asList(column.split(","));
            TableName table = MatWorkProcessSendRecordDO.class.getAnnotation(TableName.class);
            String tableName = "";
            if (table != null) {
                tableName = table.value();
            }
            iDynamicService.insertPlusRecord(tableName, columns, hashMaps, null);*/
            TableName table = MatWorkProcessSendRecordDO.class.getAnnotation(TableName.class);
            for (MatWorkProcessSendRecordVO recordVO : sendRecordVOs) {
                if (recordVO.getBeginTime().trim().equals("")) {
                    recordVO.setBeginTime(null);
                }
                if (recordVO.getEndTime().trim().equals("")) {
                    recordVO.setEndTime(null);
                }

            }
            batchSave(sendRecordVOs, table);
            //     countDownLatch.await();
        } catch (Exception e) {
            log.error("asyncBatchSaveSendRecord????????????????????????????????????????????????????????????????????????");
            log.error(e.getMessage(), e);
        }

    }

    public void batchSave(List<?> lists, TableName table) {
        List<HashMap<String, Object>> hashMaps = JsonUtil.JsonToMapList(JsonUtil.toJson(lists));
        Set<String> strings = hashMaps.get(0).keySet();
        String column = humpToLine2(StringUtils.join(strings, ","));
        String columnStr = column.replaceAll("deleted", "is_delete");
        List<String> columns = Arrays.asList(columnStr.split(","));

        String tableName = "";
        if (table != null) {
            tableName = table.value();
        }
        iDynamicService.insertPlusRecord(tableName, columns, hashMaps, null);
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


    //???????????????
    private void executeSubProcess(MatWorkProcessVO subProcessVO){
        if(subProcessVO.getId() != null && subProcessVO.getHandleId() != null){
            //??????subProcessVO????????????
            saveMarketingRule(subProcessVO);

            calculationTagGroupByProcessRule(subProcessVO);
        }
    }


    //????????????????????????????????????????????????
    private ResponseVO commonTypeMarketingRuleOperater(MatWorkProcessVO matWorkProcessVO) {
        try {
            log.info("??????????????????????????????????????????");
            List<TreeMap> treeMaps = matCommonService.changeRuleToSQLWithResult(matWorkProcessVO);//???????????????????????????????????????????????????
            List<Long> idsList = new ArrayList<>();
            for (TreeMap map : treeMaps) {
                idsList.add(Long.valueOf(map.get("member_id").toString()));
            }
            log.info("workId=???"+matWorkProcessVO.getId()+"??????????????????????????????????????????????????????"+idsList.size()+"???");
            List<MatWorkProcessHandleVO> handles = matWorkProcessVO.getHandles();
            Long workId = matWorkProcessVO.getWorksId();
            Long tagGroupId = matWorkProcessVO.getUserGroupId();
            Long matWorkId = matWorkProcessVO.getId();
            Map<Long, Long> handleIdMap = matWorkProcessVO.getHandleIdMap();

            List<MatWorkProcessHandleVO> processTypes = new ArrayList<>();
            List<MatWorkProcessHandleVO> processTypesHit = new ArrayList<>();
            for (int i = 0; i < handles.size(); i++) {
                Integer processType = handles.get(i).getProcessType();
                if (processType == 1) {//???????????????????????????handle??????
                    processTypesHit.add(handles.get(i));
                } else {//??????????????????????????????handle??????
                    processTypes.add(handles.get(i));
                }
            }

            openAbtestOperator(processTypesHit, idsList, workId, tagGroupId, matWorkId, handleIdMap, BusinessEnum.HITED.getCode());
            log.info("workId=???"+matWorkProcessVO.getId()+"??????????????????????????????????????????????????????");
            if (!processTypes.isEmpty()) {
                List<Long> memberIdsByGroupId = getMemberIdListByTagGroupId(tagGroupId);
                memberIdsByGroupId.removeAll(idsList);
                log.info("workId=???"+matWorkProcessVO.getId()+"?????????????????????????????????????????????????????????"+memberIdsByGroupId.size()+"???");
                openAbtestOperator(processTypes, memberIdsByGroupId, workId, tagGroupId, matWorkId, handleIdMap, BusinessEnum.NOTHIT.getCode());
                log.info("workId=???"+matWorkProcessVO.getId()+"?????????????????????????????????????????????????????????");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(4001, "??????????????????????????????");
        }
        return ResponseVO.success("??????????????????????????????");
    }


    //???????????????????????????????????????????????????
    private ResponseVO nothandleScreenOperator(MatWorkProcessVO matWorkProcessVO) {
        try {
            List<MatWorkProcessHandleVO> handles = matWorkProcessVO.getHandles();
            Long workId = matWorkProcessVO.getWorksId();
            Long tagGroupId = matWorkProcessVO.getUserGroupId();
            Long matWorkId = matWorkProcessVO.getId();
            Map<Long, Long> handleIdMap = matWorkProcessVO.getHandleIdMap();
            Long parentHandleId = matWorkProcessVO.getHandleId();//???????????????id

            List<Long> idsList = new ArrayList<>();
            if(parentHandleId != null && parentHandleId != 0L){
                idsList = getMemberIdListByParentHandleId(parentHandleId);//?????????????????????id??????????????????id
            }else{
                idsList = getMemberIdListByTagGroupId(tagGroupId);//????????????id??????????????????id
            }
            //??????????????????ABTest
            if (matWorkProcessVO.getIsOpenAbtest() == 0) {//????????????
                Long matHandleId = handles.get(0).getId();
                Long handleId = handleIdMap.get(matHandleId);

                saveBatchCalculationData(idsList, workId, tagGroupId, matWorkId, matHandleId, handleId, BusinessEnum.NOTHIT.getCode());

            } else if (matWorkProcessVO.getIsOpenAbtest() == 1) {//????????????
                openAbtestOperator(handles, idsList, workId, tagGroupId, matWorkId, handleIdMap, BusinessEnum.NOTHIT.getCode());
            } else {
                return ResponseVO.error(4001, "ABTest??????????????????0???1");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(4001, "??????????????????????????????");
        }

        return ResponseVO.success("?????????????????????????????????");
    }

    //?????????ABTets???handle????????????????????????????????????
    private void openAbtestOperator(List<MatWorkProcessHandleVO> handles,
                                    List<Long> idsList, Long workId, Long tagGroupId,
                                    Long matWorkId, Map<Long, Long> handleIdMap, Integer isHit) throws Exception {

        int sumSize = 0;
        int count = 0;
        List<Long> ids;
        for (int i = 0; i < handles.size(); i++) {
            Long matHandleId = handles.get(i).getId();
            Long handleId = handleIdMap.get(matHandleId);
            if (i == handles.size() - 1) {
                ids = idsList.subList(sumSize, idsList.size());
            } else {
                Double percent = handles.get(i).getPercent();
                count = (int) ((idsList.size() * percent) / 100);
                ids = idsList.subList(sumSize, sumSize + count);
            }
            saveBatchCalculationData(ids, workId, tagGroupId, matWorkId, matHandleId, handleId, isHit);
            sumSize += count;
        }
    }

    //??????????????????????????????
    public void saveBatchCalculationData(List<Long> idsList, Long workId, Long tagGroupId,
                                         Long matWorkId, Long matHandleId, Long handleId, Integer isHit) throws Exception {
        Long orgId = 0L;
        Long brandId = 0L;
        List<TreeMap> treeMaps = getWoappOrgIdAndBrandIdByTagGroupId(tagGroupId);
        if (!treeMaps.isEmpty()) {
            orgId = Long.valueOf(String.valueOf(treeMaps.get(0).get("orgId") == null ? 0 : treeMaps.get(0).get("orgId")));
            brandId = Long.valueOf(String.valueOf(treeMaps.get(0).get("brandId") == null ? 0 : treeMaps.get(0).get("brandId")));
        }

        List<TreeMap> memberInfos = new ArrayList<>();
        if (idsList != null && idsList.size() > 5000) {
            log.info("----------------idsList????????????5000???????????????-----------------");
            int n = idsList.size() / 5000;
            if (idsList.size() % 20000 != 0) {
                n = n + 1;
            }
            List<List<Long>> ids01 = averageAssign(idsList, n);
            for (int j = 0; j < ids01.size(); j++) {
                memberInfos = getMemberInfoListByTagGroupId(ids01.get(j));
                structureModel(memberInfos, workId, tagGroupId, matWorkId, matHandleId, handleId, isHit, orgId, brandId);
                log.info("----------------?????????" + j + "????????????" + ids01.get(j).size() + "-------------------");
            }
        } else {
            if (idsList != null && idsList.size() > 0) {
                memberInfos = getMemberInfoListByTagGroupId(idsList);
                structureModel(memberInfos, workId, tagGroupId, matWorkId, matHandleId, handleId, isHit, orgId, brandId);
            }
        }

        //     List<TreeMap> memberInfos = getMemberInfoListByTagGroupId(idsList);

    }


    private void structureModel(List<TreeMap> memberInfos, Long workId, Long tagGroupId, Long matWorkId, Long matHandleId, Long handleId, Integer isHit, Long orgId, Long brandId) throws Exception {
        List<MatCalculationDataDO> dataDOS = new ArrayList<>();
        for (TreeMap map : memberInfos) {

            String memberInfo = this.getMemberInfo(map);

            MatCalculationDataDO dataDO = new MatCalculationDataDO();
            dataDO.setBrandId(brandId);
            dataDO.setMatHandleId(matHandleId);
            dataDO.setMatWorkId(matWorkId);
            dataDO.setMemberId(Long.valueOf(map.get("member_id").toString()));
            dataDO.setOriginalId(orgId);
            dataDO.setTagGroupId(tagGroupId);
            dataDO.setWorkId(workId);
            dataDO.setHandleId(handleId);
            dataDO.setIsHit(isHit);
            dataDO.setMemberInfo(memberInfo);
            dataDO.setDeleted(BusinessEnum.NOTDELETED.getCode());
            dataDOS.add(dataDO);
        }
        //   this.saveBatch(dataDOS);//????????????
        TableName table = MatCalculationDataDO.class.getAnnotation(TableName.class);
        batchSave(dataDOS, table);
    }

    //???????????????list????????????
    private <T> List<List<T>> averageAssign(List<T> data, int count) throws Exception {
        List<List<T>> result = new LinkedList<>();
        int num = data.size() / count;//??????????????????
        for (int i = 0; i < count; i++) {
            List<T> list = new ArrayList<>();
            if (i < count - 1) {
                list = data.subList(i * num, (i + 1) * num);
            } else {//?????????????????????????????????????????????????????????????????????????????????
                list = data.subList(i * num, data.size());
            }
            result.add(list);
        }
        return result;
    }

    //????????????????????????
    public String getMemberInfo(TreeMap map) throws Exception {

        JSONArray jsonArr = new JSONArray();
        JSONObject mobileObj = new JSONObject();
        mobileObj.put("platform", "mobile");
        mobileObj.put("attribute", map.get("mobile") == null ? "" : map.get("mobile"));
        jsonArr.add(mobileObj);
        JSONObject emailObj = new JSONObject();
        emailObj.put("platform", "email");
        emailObj.put("attribute", map.get("email") == null ? "" : map.get("email"));
        jsonArr.add(emailObj);
        JSONObject qywxObj = new JSONObject();
        qywxObj.put("platform", "qywx");
        qywxObj.put("attribute", "");
        jsonArr.add(qywxObj);
        JSONObject memberObj = new JSONObject();
        memberObj.put("platform", "member");
        memberObj.put("attribute", map.get("member_id") == null ? "" : map.get("member_id"));
        jsonArr.add(memberObj);
        JSONObject wechatObj = new JSONObject();
        wechatObj.put("platform", "wechat");
        wechatObj.put("appid", map.get("wechat_appid") == null ? "" : map.get("wechat_appid"));
        wechatObj.put("attribute", map.get("wechat_openid") == null ? "" : map.get("wechat_openid"));
        jsonArr.add(wechatObj);
        JSONObject miniappObj = new JSONObject();
        miniappObj.put("platform", "miniapp");
        miniappObj.put("appid", "");
        miniappObj.put("attribute", "");
        jsonArr.add(miniappObj);

        return jsonArr.toJSONString();
    }

    //????????????id??????????????????id
    private List<TreeMap> getWoappOrgIdAndBrandIdByTagGroupId(Long tagGroupId) throws Exception {
        //select o.woaap_org_id orgId,w.woaap_id brandId from sys_tag_group t,sys_brands b,sys_brands_org o,sys_woaap_brands w where t.brands_id=b.id and b.org_id=o.id and b.id=w.brands_id and t.id=10;
        List<String> tableNames = new ArrayList<>();
        tableNames.add("sys_tag_group t");
        tableNames.add("sys_brands b");
        tableNames.add("sys_brands_org o");
        tableNames.add("sys_woaap_brands w");
        List<String> columns = new ArrayList<>();
        columns.add("o.woaap_org_id orgId");
        columns.add("w.woaap_id brandId");
        String whereClause = "t.brands_id=b.id and b.org_id=o.id and b.id=w.brands_id and t.id=" + tagGroupId;
        List<TreeMap> treeMaps = iDynamicService.selectList(tableNames, columns, whereClause, null);
        return treeMaps;
    }

    //????????????id????????????????????????
    private List<TreeMap> getMemberInfoListByTagGroupId(List<Long> idsList) throws Exception {
        String idList = "";
        for (int i = 0; i < idsList.size(); i++) {
            if (i != (idsList.size() - 1)) {
                idList += "'" + idsList.get(i) + "'" + ",";
            } else {
                idList += "'" + idsList.get(i) + "'";
            }
        }
        //SELECT m.id,m.member_id,m.unionid,m.email,m.mobile,m.is_delete FROM members m WHERE m.member_id in ()
        List<String> tableNames = new ArrayList<>();
        tableNames.add("members m");
        List<String> columns = new ArrayList<>();
        columns.add("m.member_id");
        columns.add("m.unionid");
        columns.add("m.email");
        columns.add("m.mobile");
        //    columns.add("m.is_delete");
        String whereClause = " m.member_id in (" + idList + ") GROUP BY m.member_id";
        List<TreeMap> mapList = iDynamicService.selectListMat(
                tableNames, columns, whereClause, "", null, null, null,null);
        return mapList;
    }


    private List<Long> getMemberIdListByParentHandleId(Long parentHandleId){
        List<String> tableNames = new ArrayList<>();
        tableNames.add("mat_map_calculation_data s");
        List<String> columns = new ArrayList<>();
        columns.add("s.member_id");
        String whereClause = "s.mat_handle_id=" + parentHandleId + " group by member_id";
        List<Long> idsList = iDynamicService.getIdsList(tableNames, columns, whereClause, null);
        return idsList;
    }

    //????????????id??????????????????id
    private List<Long> getMemberIdListByTagGroupId(Long tagGroupId) throws Exception {
        //select m.member_id from members m,sys_tag_group_user s where s.user_id=m.id and s.tag_group_id=9;
        List<String> tableNames = new ArrayList<>();
        tableNames.add("members m");
        tableNames.add("sys_tag_group_user s");
        List<String> columns = new ArrayList<>();
        columns.add("m.member_id");
        String whereClause = "s.user_id=m.id and s.tag_group_id=" + tagGroupId;
        List<Long> idsList = iDynamicService.getIdsList(tableNames, columns, whereClause, null);
        return idsList;
    }


}








