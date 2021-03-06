package org.etocrm.authentication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.etocrm.authentication.entity.DO.SysBrandsDO;
import org.etocrm.authentication.entity.DO.SysBrandsOrgDO;
import org.etocrm.authentication.entity.DO.SysPermissionDO;
import org.etocrm.authentication.entity.DO.SysUserDO;
import org.etocrm.authentication.entity.VO.SysUserAllVO;
import org.etocrm.authentication.entity.VO.auth.*;
import org.etocrm.authentication.mapper.ISysBrandsMapper;
import org.etocrm.authentication.mapper.ISysBrandsOrgMapper;
import org.etocrm.authentication.mapper.ISysUserMapper;
import org.etocrm.authentication.service.ISysMenuService;
import org.etocrm.authentication.service.ISysPermissionService;
import org.etocrm.authentication.service.ISysUserService;
import org.etocrm.core.enums.BusinessEnum;
import org.etocrm.core.enums.ResponseEnum;
import org.etocrm.core.util.*;
import org.etocrm.dynamicDataSource.util.BasePage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author peter.li
 * @date 2020/8/17 14:20
 */
@Service
@Slf4j
public class SysUserServiceImpl implements ISysUserService {

    @Autowired
    private ISysUserMapper iSysUserMapper;

    @Autowired
    private SysMenuCacheManager sysMenuCacheManager;

    @Autowired
    private ISysPermissionService iSysPermissionService;

    @Autowired
    private ISysMenuService sysMenuService;
    @Autowired
    private ISysBrandsMapper iSysBrandsMapper;
    @Autowired
    private ISysBrandsOrgMapper iSysBrandsOrgMapper;

    /**
     * ????????????????????????
     *
     * @return
     */
    @Override
    public ResponseVO getUsersByPage(SysUserInPageVO userVO) {
        SysUserDO sysUserDO = new SysUserDO();
        List<SysUserOutVO> list = new ArrayList<>();
        try {
            ParamDeal.setStringNullValue(userVO);

            IPage<SysUserDO> iPage = new Page<>(VoParameterUtils.getCurrent(userVO.getCurrent()), VoParameterUtils.getSize(userVO.getSize()));
            BeanUtils.copyPropertiesIgnoreNull(userVO, sysUserDO);
            LambdaQueryWrapper<SysUserDO> lQuery = conditionDecide(sysUserDO);

            IPage<SysUserDO> sysRoleDOIPage = iSysUserMapper.selectPage(iPage, lQuery);
            BasePage page = new BasePage(sysRoleDOIPage);
            SysUserOutVO sysUserOutVO;
            for (SysUserDO sysUserDO1 : sysRoleDOIPage.getRecords()) {
                sysUserOutVO = new SysUserOutVO();
                BeanUtils.copyPropertiesIgnoreNull(sysUserDO1, sysUserOutVO);
                SysBrandsDO sysBrandsDO = iSysBrandsMapper.selectById(sysUserDO1.getBrandsId());
                SysBrandsOrgDO sysBrandsOrgDO = iSysBrandsOrgMapper.selectById(sysUserDO1.getOrganization());
                SysUserDO userDO1 = new SysUserDO();
                userDO1.setId(sysUserDO1.getCreatedBy()==null?1:sysUserDO1.getCreatedBy());
                SysUserDO userDO = iSysUserMapper.selectById(userDO1);
                if(sysBrandsDO!=null){
                    sysUserOutVO.setBrandsName(sysBrandsDO.getBrandsName()==null?"":sysBrandsDO.getBrandsName());
                }
                if(sysBrandsOrgDO!=null){
                    sysUserOutVO.setOrganizationName(sysBrandsOrgDO.getOrgName()==null?"":sysBrandsOrgDO.getOrgName());
                }
                sysUserOutVO.setCreatedByName(userDO.getUserName());

                sysUserOutVO.setCreatedTime(DateUtil.formatDateTimeByFormat(sysUserDO1.getCreatedTime(),DateUtil.default_datetimeformat));

                list.add(sysUserOutVO);
            }
            page.setRecords(list);
            return ResponseVO.success(page);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_GET_ERROR);
        }
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @Override
    public ResponseVO findAll() {
        SysUserDO sysUserDO = new SysUserDO();
        List<SysUserOutVO> sysUserOutVOs = new ArrayList<>();
        try {
            LambdaQueryWrapper<SysUserDO> queryWrapper = new LambdaQueryWrapper<>(sysUserDO)
                    .eq(SysUserDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                    .orderByDesc(SysUserDO::getCreatedTime);
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(queryWrapper);
            for (SysUserDO sysUserDO1 : sysUserDOs) {
                SysUserOutVO sysUserOutVO = new SysUserOutVO();
                BeanUtils.copyPropertiesIgnoreNull(sysUserDO1, sysUserOutVO);
                SysBrandsDO sysBrandsDO = iSysBrandsMapper.selectById(sysUserDO1.getBrandsId());
                SysBrandsOrgDO sysBrandsOrgDO = iSysBrandsOrgMapper.selectById(sysUserDO1.getOrganization());
                if(sysBrandsDO!=null){
                    sysUserOutVO.setBrandsName(sysBrandsDO.getBrandsName()==null?"":sysBrandsDO.getBrandsName());
                }
                if(sysBrandsOrgDO!=null){
                    sysUserOutVO.setOrganizationName(sysBrandsOrgDO.getOrgName()==null?"":sysBrandsOrgDO.getOrgName());
                }
                sysUserOutVOs.add(sysUserOutVO);
            }

            return ResponseVO.success(sysUserOutVOs);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_GET_ERROR);
        }
    }


    /**
     * ??????id??????????????????
     *
     * @param id
     * @return
     */
    @Override
    public ResponseVO getSysUserById(Integer id) {
        try {
            SysUserDO sysUserDO = iSysUserMapper.selectById(id);
            SysUserOutVO sysUserOutVO = new SysUserOutVO();
            BeanUtils.copyPropertiesIgnoreNull(sysUserDO, sysUserOutVO);
            SysBrandsDO sysBrandsDO = iSysBrandsMapper.selectById(sysUserDO.getBrandsId());
            SysBrandsOrgDO sysBrandsOrgDO = iSysBrandsOrgMapper.selectById(sysUserDO.getOrganization());
            if(sysBrandsDO!=null){
                sysUserOutVO.setBrandsName(sysBrandsDO.getBrandsName()==null?"":sysBrandsDO.getBrandsName());
            }
            if(sysBrandsOrgDO!=null){
                sysUserOutVO.setOrganizationName(sysBrandsOrgDO.getOrgName()==null?"":sysBrandsOrgDO.getOrgName());
            }
            SysUserDO userDO1 = new SysUserDO();
            userDO1.setId(sysUserDO.getCreatedBy()==null?1:sysUserDO.getCreatedBy());
            SysUserDO userDO = iSysUserMapper.selectById(userDO1);

            sysUserOutVO.setCreatedByName(userDO.getUserName());

            sysUserOutVO.setCreatedTime(DateUtil.formatDateTimeByFormat(sysUserDO.getCreatedTime(),DateUtil.default_datetimeformat));

            //       List<SysPermissionDO> sysPermissionDOs = iSysPermissionService.getSysPermissionsByUserId(id);
            //       sysUserOutVO.setSysPerms(sysPermissionDOs);
            return ResponseVO.success(sysUserOutVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_GET_ERROR);
        }
    }

    /**
     * ??????????????????
     *
     * @param userVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO saveSysUser(SysUserAddVO userVO) {
        try {
            if(userVO.getUserName()!=null&&userVO.getUserName().length()>32){
                return ResponseVO.error(4001,"????????????????????????????????????32");
            }
            //??????????????????
            String eamil = userVO.getEmail();
            if (!eamil.matches("[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                return ResponseVO.error(ResponseEnum.DATA_SOURCE_EMAIL_ERROR);
            }
            //?????????????????????
            String phone = userVO.getPhone();
            if (!phone.matches("1\\d{10}$")) {
                return ResponseVO.error(ResponseEnum.DATA_SOURCE_PHONE_ERROR);
            }


            QueryWrapper<SysUserDO> query = new QueryWrapper();
            query.eq("is_delete", BusinessEnum.NOTDELETED.getCode());
            query.and(wrapper -> wrapper.eq("user_account",userVO.getUserAccount().trim())
                    .or().eq("sso_id", userVO.getSsoId()).or().eq("email",userVO.getEmail().trim()));
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(query);
            if (sysUserDOs!=null&&sysUserDOs.size() > 0) {
                return ResponseVO.error(4001,"??????????????????????????????ID????????????????????????????????????");
            }
            SysUserDO sysUserDO = new SysUserDO();
            BeanUtils.copyPropertiesIgnoreNull(userVO, sysUserDO);
            sysUserDO.setUserAccount(userVO.getUserAccount().trim());
            //???????????????????????????????????????????????????
           /* if(sysUserDO.getUserAccount().equals("admin")){
                sysUserDO.setPassword(new BCryptPasswordEncoder().encode("etocrm@admin"));
            }*/
            sysUserDO.setPassword(new BCryptPasswordEncoder().encode("123456"));
     //       sysUserDO.setPassword(new BCryptPasswordEncoder().encode(sysUserDO.getPassword()));
            iSysUserMapper.insert(sysUserDO);

            return ResponseVO.success(sysUserDO.getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_ADD_ERROR);
        }

    }

    /**
     * ??????????????????
     *
     * @param userVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO updateSysUser(SysUserUpdateInfoVO userVO) {
        try {

            //??????????????????????????????
            /*Boolean isSupperUser = checkUserAllowed(new SysUserDO(userVO.getId()));
            if (isSupperUser) {
                return ResponseVO.error(4001,"???????????????????????????");
            }*/
            SysUserDO userDO = iSysUserMapper.selectById(userVO.getId());
            if(userDO.getUserAccount().equals("admin")){
                return ResponseVO.error(4001,"?????????????????????????????????");
            }

            if(userVO.getUserName()!=null&&userVO.getUserName().length()>32){
                return ResponseVO.error(4001,"????????????????????????????????????32");
            }

            //update?????????user_account???sso_id???email????????????
            String returnResult = checkUniqueness(userVO);
            if(!returnResult.equals("")){
                return ResponseVO.error(4001,returnResult+"???????????????????????????");
            }

            SysUserDO sysUserDO = new SysUserDO();
            BeanUtils.copyPropertiesIgnoreNull(userVO, sysUserDO);
            if(userVO.getUserAccount() != null){
                sysUserDO.setUserAccount(userVO.getUserAccount().trim());
            }
            iSysUserMapper.updateById(sysUserDO);

            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_UPDATE_ERROR);
        }
    }

    //????????????????????????
    private String checkUniqueness(SysUserUpdateInfoVO userVO){
        String returnResult ="";
        QueryWrapper<SysUserDO> query = new QueryWrapper();

        if(StringUtils.isNotBlank(userVO.getUserAccount())){
            query.eq("user_account",userVO.getUserAccount().trim());
            query.eq("is_delete", BusinessEnum.NOTDELETED.getCode());
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(query);
            if(sysUserDOs.size() > 0){
                if(!sysUserDOs.get(0).getId().equals(userVO.getId())){
                    returnResult = "?????????";
                }
            }
        }

        if(userVO.getSsoId()!=null){
            query = new QueryWrapper();
            query.eq("sso_id",userVO.getSsoId());
            query.eq("is_delete", BusinessEnum.NOTDELETED.getCode());
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(query);
            if(sysUserDOs.size() > 0){
                if(!sysUserDOs.get(0).getId().equals(userVO.getId())){
                    if(!returnResult.equals("")){
                        returnResult = returnResult + "?????????????????????ID";
                    }else{
                        returnResult = "??????????????????ID";
                    }
                }
            }
        }

        if(StringUtils.isNotBlank(userVO.getEmail())){
            query = new QueryWrapper();
            query.eq("email",userVO.getEmail());
            query.eq("is_delete", BusinessEnum.NOTDELETED.getCode());
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(query);
            if(sysUserDOs.size() > 0){
                if(!sysUserDOs.get(0).getId().equals(userVO.getId())){
                    if(!returnResult.equals("")){
                        returnResult = returnResult + "?????????";
                    }else{
                        returnResult = "??????";
                    }
                }
            }
        }
        return returnResult;
    }

    /**
     * ??????id??????????????????
     *
     * @param userVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO updateUserAuth(SysUserUpdateAuthVO userVO) {
        try {

            SysUserDO userDO = iSysUserMapper.selectById(userVO.getId());
            if(userDO.getUserAccount().equals("admin")){
                return ResponseVO.error(4001,"?????????????????????????????????");
            }
            //????????????id??????????????????
            List<SysPermissionDO> sysPermissionsDOs = iSysPermissionService.getSysPermissionsByUserId(userVO.getId());
            List<Integer> roleIds = new ArrayList<>();
            if(sysPermissionsDOs!=null&&sysPermissionsDOs.size()>0){
                for(SysPermissionDO sysPermissionDO : sysPermissionsDOs){
                    roleIds.add(sysPermissionDO.getRoleId());
                }
            }
            List<Integer> roleIdsNew = new ArrayList<>(userVO.getRoleIds());
            //???????????????????????????
            roleIdsNew.removeAll(roleIds);
            if(roleIdsNew.size() > 0){
                //????????????????????????????????????
                saveUpdateRelations(roleIdsNew,userVO.getId());
            }
            //???????????????????????????
            roleIds.removeAll(userVO.getRoleIds());
            if(roleIds.size() > 0){
                //??????????????????????????????
                for(Integer roleId : roleIds){
                    iSysPermissionService.removeByRoleId(roleId,userVO.getId());
                }
            }

            //?????????????????????????????????
            sysMenuCacheManager.removeAuthorizedMenuTree(userVO.getId());

         //   sysMenuCacheManager.removeButtonPerms(userVO.getId());
            //????????????????????????
            sysMenuCacheManager.removeMenuIds(userVO.getId());

            //??????????????????????????????
            List<String> buttonPerms = sysMenuService.getUrlPermission(userVO.getId());
            sysMenuCacheManager.cacheButtonPerms(userVO.getId(), buttonPerms);

            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_UPDATE_ERROR);
        }
    }

    /**
     * ????????????????????????
     *
     * @param userVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO updatePassword(SysUserPwdVO userVO) {
        try {

            if(userVO.getUserAccount().equals("admin")){
                return ResponseVO.error(4001,"?????????????????????????????????");
            }

            //??????????????????????????????
            /*Boolean isSupperUser = checkUserAllowed(new SysUserDO(userVO.getId()));
            if (isSupperUser) {
                return ResponseVO.error(4001,"???????????????????????????");
            }*/

            SysUserDO sysUserDO = new SysUserDO();
            BeanUtils.copyPropertiesIgnoreNull(userVO, sysUserDO);
            //            sysUserDO.setPassword(bCryptPasswordEncoder.encode(userVO.getPassword()));
            iSysUserMapper.updateById(sysUserDO);
            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_UPDATE_ERROR);
        }
    }

    /**
     * ??????id??????????????????
     *
     * @param id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO removeSysUser(Integer id) {
        try {
            //??????????????????????????????
            /*Boolean isSupperUser = checkUserAllowed(new SysUserDO(id));
            if (isSupperUser) {
                return ResponseVO.error(4001,"???????????????????????????");
            }*/

            SysUserDO sysUserDO = new SysUserDO();
            sysUserDO.setId(id);
            sysUserDO.setDeleted(BusinessEnum.DELETED.getCode());
            iSysUserMapper.updateById(sysUserDO);

            iSysPermissionService.removeByUserId(id);

            sysMenuCacheManager.removeAuthorizedMenuTree(id);
            sysMenuCacheManager.removeButtonPerms(id);
            sysMenuCacheManager.removeMenuIds(id);
            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_REMOVE_ERROR);
        }
    }

    /**
     * ????????????/??????,1???????????????,0???????????????
     *
     * @param sysUserChangeStatusVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO changeStatus(SysUserChangeStatusVO sysUserChangeStatusVO) {
        try {

            SysUserDO userDO = iSysUserMapper.selectById(sysUserChangeStatusVO.getId());
            if(userDO.getUserAccount().equals("admin")){
                return ResponseVO.error(4001,"?????????????????????????????????");
            }

            List<SysPermissionDO> relationList = iSysPermissionService
                    .getSysPermissionsByUserId(sysUserChangeStatusVO.getId());
            if(relationList!=null&&relationList.size()>0){
                List<Integer> roleIds = new ArrayList<>();
                for(SysPermissionDO sysPermissionDO : relationList){
                    roleIds.add(sysPermissionDO.getRoleId());
                }
                if(roleIds.contains(1)){
                    return ResponseVO.error(4001,"???????????????????????????");
                }
            }

            //??????????????????????????????
            /*Boolean isSupperUser = checkUserAllowed(new SysUserDO(sysUserChangeStatusVO.getId()));
            if (isSupperUser) {
                return ResponseVO.error(4001,"???????????????????????????");
            }*/

            SysUserDO sysUserDO = new SysUserDO();
            sysUserDO.setId(sysUserChangeStatusVO.getId());
            sysUserDO.setStatus(sysUserChangeStatusVO.getStatus());
            iSysUserMapper.updateById(sysUserDO);

            //??????????????????????????????
       //     iSysPermissionService.removeByUserId(sysUserChangeStatusVO.getId());
            //??????????????????????????????
            sysMenuCacheManager.removeButtonPerms(sysUserChangeStatusVO.getId());
            sysMenuCacheManager.removeMenuIds(sysUserChangeStatusVO.getId());
            return ResponseVO.success();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_UPDATE_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(String userAccount) {
        try {
            UpdateWrapper<SysUserDO> query = new UpdateWrapper<>();
            query.eq("user_account",userAccount);
            query.eq("is_delete",BusinessEnum.NOTDELETED.getCode());
            SysUserDO userDO = new SysUserDO();
            userDO.setLastLoginTime(new Date());
            iSysUserMapper.update(userDO,query);
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @Override
    public ResponseVO findUserAll(SysUserAllVO userVO) {
        List<SysUserOutVO> sysUserOutVOs = new ArrayList<>();
        try {
            LambdaQueryWrapper<SysUserDO> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
            if (null != userVO.getUserAccount()){
                objectLambdaQueryWrapper.like(SysUserDO::getUserAccount,userVO.getUserAccount());
            }
            if (null != userVO.getUserName()){
                objectLambdaQueryWrapper.like(SysUserDO::getUserName,userVO.getUserName());
            }
            objectLambdaQueryWrapper.eq(SysUserDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                    .orderByDesc(SysUserDO::getCreatedTime);
            List<SysUserDO> sysUserDOs = iSysUserMapper.selectList(objectLambdaQueryWrapper);
            for (SysUserDO sysUserDO1 : sysUserDOs) {
                SysUserOutVO sysUserOutVO = new SysUserOutVO();
                BeanUtils.copyPropertiesIgnoreNull(sysUserDO1, sysUserOutVO);
                SysBrandsDO sysBrandsDO = iSysBrandsMapper.selectById(sysUserDO1.getBrandsId());
                SysBrandsOrgDO sysBrandsOrgDO = iSysBrandsOrgMapper.selectById(sysUserDO1.getOrganization());
                SysUserDO userDO1 = new SysUserDO();
                userDO1.setId(sysUserDO1.getCreatedBy()==null?1:sysUserDO1.getCreatedBy());
                SysUserDO userDO = iSysUserMapper.selectById(userDO1);
                if(sysBrandsDO!=null){
                    sysUserOutVO.setBrandsName(sysBrandsDO.getBrandsName()==null?"":sysBrandsDO.getBrandsName());
                }
                if(sysBrandsOrgDO!=null){
                    sysUserOutVO.setOrganizationName(sysBrandsOrgDO.getOrgName()==null?"":sysBrandsOrgDO.getOrgName());
                }
                sysUserOutVO.setCreatedByName(userDO.getUserName());

                sysUserOutVO.setCreatedTime(DateUtil.formatDateTimeByFormat(sysUserDO1.getCreatedTime(),DateUtil.default_datetimeformat));

                sysUserOutVOs.add(sysUserOutVO);
            }
            return ResponseVO.success(sysUserOutVOs);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseVO.error(ResponseEnum.DATA_SOURCE_GET_ERROR);
        }
    }

    @Override
    public SysUserDO getOne(QueryWrapper<SysUserDO> query) {
        try {
            SysUserDO sysUserDO = iSysUserMapper.selectOne(query);
            if (sysUserDO == null) {
                return null;
            }
            return sysUserDO;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    //    private void saveRelations(SysUserAddVO sysUserAddVO,Integer id) throws Exception{
    //        if(sysUserAddVO.getSysPerms().size() > 0){
    //            for(SysPermissionDO sysPermissionDO : sysUserAddVO.getSysPerms()){
    //                sysPermissionDO.setUserId(id);
    //                iSysPermissionService.saveSysPermission(sysPermissionDO);
    //            }
    //        }
    //    }

    private void saveUpdateRelations(List<Integer> roleIds,Integer userId) throws Exception {
        if (roleIds.size() > 0) {
            for (Integer roleId : roleIds) {
                SysPermissionDO sysPermissionDO = new SysPermissionDO();
                sysPermissionDO.setUserId(userId);
                sysPermissionDO.setRoleId(roleId);
                iSysPermissionService.saveSysPermission(sysPermissionDO);
            }
        }
    }

    private LambdaQueryWrapper<SysUserDO> conditionDecide(SysUserDO userDO) throws Exception {

        LambdaQueryWrapper<SysUserDO> lQuery = new LambdaQueryWrapper<>();
        if (userDO.getUserName() != null) {
            lQuery.like(SysUserDO::getUserName, userDO.getUserName());
        }
        lQuery
                .eq(SysUserDO::getDeleted, BusinessEnum.NOTDELETED.getCode())
                .orderByDesc(SysUserDO::getId);

        return lQuery;
    }

    public Boolean checkUserAllowed(SysUserDO userDO) throws Exception {
        if (userDO.isAdmin()) {
            return true;
        }
        return false;
    }

    public ResponseVO refreshMenuIdsByUserId(Integer userId){
        try {
            List<String> menuIds = sysMenuService.getMenusByUserId(userId);
            return ResponseVO.success(menuIds);
        }catch (Exception e){
            log.error(e.getMessage(), e);
            return ResponseVO.error(4001,"??????????????????");
        }
    }
}
