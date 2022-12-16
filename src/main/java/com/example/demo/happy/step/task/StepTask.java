package com.example.demo.happy.step.task;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.happy.step.utils.ResourceUtil;
import com.example.demo.happy.step.utils.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 * StepTask
 * </p>
 *
 * @author zhenghao
 * @date 2020/9/18 15:56
 */
@Configuration
@EnableScheduling
@Slf4j
//@PropertySource(value = "classpath:/application.properties")
public class StepTask {
    private static int reCount = 2;

    @Value("${step.login.account}")
    private String account;
    @Value("${step.login.password}")
    private String password;
    @Value("${token.file.name:}")
    private String tokenFilePath;
    @Value("${step.login.minStep:}")
    private Integer minStep;
    @Value("${step.wait.minute:}")
    private Integer waitMinute;
    @Value("${step.login.everyMin:}")
    private Integer everyMin;
    @Value("${step.login.everyAdd:}")
    private Integer everyAdd;

    //    @Scheduled(cron = "0 0 18 * * ?")
//    @Scheduled(cron = "0 0/30 8-20 * * ?")
    @Scheduled(cron = "${step.schedule.cron}")
//    @Scheduled(cron = "0 * 13 * * ?")
    public void task2() throws InterruptedException {
        int nextInt = new Random().nextInt(waitMinute);
        log.info("【准备同步步数】等待{}分钟------------------------------------", nextInt);
//        Thread.sleep(1000L * 60 * nextInt);
        int step = getStep();
        Map<String, String> tokenMap = readToken2();
        if (tokenMap == null || tokenMap.size() < 2 || isNull(tokenMap.get("appToken")) || isNull(tokenMap.get("userId"))) {
            reUpdate2(step);
            return;
        }
        String res = submitStep(step, tokenMap.get("userId"), tokenMap.get("appToken"));
        if (res.contains("success")) {
            log.info("==============【同步步数成功】 步数={}", step);
        } else {
            log.info("==============【同步步数失败】\n尝试重新登录并同步。。。。。");
            reUpdate2(step);
        }

    }
//    public void task() throws InterruptedException {
//        int nextInt = new Random().nextInt(waitMinute);
//        log.info("【准备同步步数】等待{}分钟------------------------------------", nextInt);
//        Thread.sleep(1000L * 60 * nextInt);
//        int step = getStep();
//        Map<String, String> tokenMap = readToken();
//        if (tokenMap == null || tokenMap.size() < 2 || isNull(tokenMap.get("accessToken")) || isNull(tokenMap.get("userId"))) {
//            reUpdate(step);
//            return;
//        }
//        JSONObject updateJson = JSONObject.parseObject(update(tokenMap.get("accessToken"), tokenMap.get("userId"), step));
//        if (isSuccess(updateJson)) {
//            log.info("==============【同步步数成功】 步数={}", step);
//        } else {
//            log.info("==============【同步步数失败】\n尝试重新登录并同步。。。。。");
//            reUpdate(step);
//        }
//
//    }

    private Integer getStep() {
        // 获取最小步数
        minStep = minStep == null ? 1 : minStep;
        // 最小增量
        everyMin = everyMin == null ? 30 : everyMin;
        everyAdd = everyAdd == null ? 3000 : everyAdd; // 每小时增量
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY) - 7;
        int minute = cal.get(Calendar.MINUTE) / everyMin;
        int addStep = hour * everyAdd + minute * everyAdd / 2;
        return new Random().nextInt(everyAdd / 2) + addStep + minStep;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    private boolean isNull(String str) {
        return isBlank(str) || str.trim().equalsIgnoreCase("null");
    }

    private void reUpdate2(int step) {
        JSONObject jsonObj = JSONObject.parseObject(login2());
        JSONObject tokenInfo = jsonObj.getJSONObject("token_info");
        String loginToken = tokenInfo.getString("login_token");
        String userId = tokenInfo.getString("user_id");
        String appToken = tokenInfo.getString("app_token");
        if (StrUtil.isNotBlank(appToken)) {
            log.info("==============【登录成功】====================");
            writeToken2(appToken, userId);
            log.info("【登录成功】写入：appToken={}, userId={}", appToken, userId);
            String res = submitStep(step, userId, appToken);
            if (res.contains("success")) {
                log.info("==============【同步步数成功】 步数={}", step);
                return;
            } else {
                log.info("==============【同步步数失败】result={}", res);
            }
        } else {
            log.info("==============【重新登录失败】---------");
        }
        if (--reCount > 0) {
            log.info("==============【同步步数失败】准备重新尝试.................");
            reUpdate2(step);
        } else {
            reCount = 2;
        }
    }
    private void reUpdate(int step) {
        JSONObject loginJson = JSONObject.parseObject(login());
        if (isSuccess(loginJson)) {
            JSONObject dataJson = loginJson.getJSONObject("data");
            String accessToken = dataJson.getString("accessToken");
            String userId = dataJson.getString("userId");
            log.info("==============【登录成功】====================");
            writeToken(accessToken, userId);
            log.info("【登录成功】写入：accessToken={}, userId={}", accessToken, userId);
            JSONObject updateJson = JSONObject.parseObject(update(accessToken, userId, step));
            if (isSuccess(updateJson)) {
                log.info("==============【同步步数成功】 步数={}", step);
                return;
            } else {
                log.info("==============【同步步数失败】result={}", updateJson);
            }
        } else {
            log.info("==============【重新登录失败】---------");
        }
        if (--reCount > 0) {
            log.info("==============【同步步数失败】准备重新尝试.................");
            reUpdate(step);
        } else {
            reCount = 2;
        }
    }

    /**
     * 状态码是否为200
     *
     * @param jsonObject json结果
     * @return 是否
     */
    private boolean isSuccess(JSONObject jsonObject) {
        return jsonObject != null && jsonObject.getInteger("code").equals(200);
    }

    /**
     * 更新步数
     *
     * @param accessToken token
     * @param userId      用户id
     * @param step        步数
     * @return 更新结果 如：{"msg":"成功","code":200,"data":{"pedometerRecordHourlyList":[{"distance":"0,0,0,0,0,0,0,17925.00,0,0,18723.00,19206.00,10673.00,11704.00,0,11737.00,0,0,0,0,0,0,0,0","created":"2020-09-18 07:44:23","measurementTime":"2020-09-18 00:00:00","active":0,"step":"0,0,0,0,0,0,0,29876,0,0,31205,32010,32020,35112,0,35212,0,0,0,0,0,0,0,0","id":"1822cd8ababb4174be5a89c68bfab39f","calories":"0,0,0,0,0,0,0,746.00,0,0,780.00,800.00,8005.00,8778.00,0,8803.00,0,0,0,0,0,0,0,0","userId":27231098,"deviceId":"M_NULL","dataSource":2,"updated":1600412486817}]}}
     */
    private String update(String accessToken, String userId, int step) {
        String updateUrl = "https://sports.lifesense.com/sport_service/sport/sport/uploadMobileStepV2?version=4.5&systemType=2";
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json; charset=utf-8");
        header.put("Cookie", "accessToken=" + accessToken);
        String params = "{\"list\":[{\"DataSource\":2,\"active\":1,\"calories\":\"" + step / 4 + "\"," +
                "\"dataSource\":2,\"deviceId\":\"M_NULL\",\"distance\":" + step / 3 + ",\"exerciseTime\":0,\"isUpload\":0," +
                "\"measurementTime\":\"" + sdf.format(now) + "\",\"priority\":0,\"step\":" + step + "," +
                "\"type\":2,\"updated\":" + now.getTime() + ",\"userId\":" + userId + "}]};";
        log.info("============== 【更新步数参数】 updateUrl={}, params={}, header={}", updateUrl, params, header);
        String result = doPost(updateUrl, params, header);
        log.info("============== 【更新步数结果】result={}", result);
        return result;
    }

    /**
     * 登录
     *
     * @return 结果
     */
    private String login() {
        String loginUrl = "https://sports.lifesense.com/sessions_service/login?systemType=2&version=4.6.7";
        String md5Pass = DigestUtils.md5DigestAsHex(password.getBytes());
        String params = "{\"appType\":6,\"clientId\":\"88888\",\"loginName\":\"" + account + "\",\"password\":\"" + md5Pass + "\",\"roleType\":0}";
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json; charset=utf-8");
        log.info("==============【登录接口参数】loginUrl={}, params={}, header={}", loginUrl, params, header);
        String result = doPost(loginUrl, params, header);
        log.info("==============【登录接口结果】result={}", result);
        return result;
    }

    public String login2() {
        String loginUrl = "https://api-user.huami.com/registrations/+86" + account + "/tokens";
        String params = "client_id=HuaMi&password=" + password + "&redirect_uri=https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fhm-registration%2Fsuccesssignin.html&token=access";
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        header.put("User-Agent", "MiFit/4.6.0 (iPhone; iOS 14.0.1; Scale/2.00)");
        log.info("==============【登录接口参数】loginUrl={}, params={}, header={}", loginUrl, params, header);
//        String result = doPost(loginUrl, params, header);
        String access;
        try (HttpResponse res = HttpUtil.createPost(loginUrl)
                .addHeaders(header)
                .body(params).execute()) {
            String location = res.header("Location");
            if (location == null || location.contains("error")) {
                log.info("登陆失败,res：{}", res);
                return null;
            }
            access = location.split("&")[1].substring(7);
        }
        // https://s3-us-west-2.amazonaws.com/hm-registration/successsignin.html?region=cn-northwest-1&
        // access=NQVBQDZOQmJaR0YyajYmWnJoBAgAAAAAAYT1OTVBEdkM1b1Y4OEg5WEVybDJmenZYQUFBQVhWOG1heFAmcj03JnQ9aHVhbWkmdGk9Kzg2MTUwOTA1MDgyMzQmaD0xNjY5ODgwNzI3MTY5Jmk9ODY0MDAwADS9PKWD9YVnkhvvW_QJHQ&country_code=CN&expiration=1670744727
//        access = "NQVBQDZOQmJaR0YyajYmWnJoBAgAAAAAAYT1OTVBEdkM1b1Y4OEg5WEVybDJmenZYQUFBQVhWOG1heFAmcj03JnQ9aHVhbWkmdGk9Kzg2MTUwOTA1MDgyMzQmaD0xNjY5ODg4NTAxMTkzJmk9ODY0MDAwsaB-hhF_VrEauf7q1ara-A";
        try (HttpResponse loginRes = HttpUtil.createPost("https://account.huami.com/v2/client/login")
                .addHeaders(header)
                .body("app_name=com.xiaomi.hm.health&app_version=4.6.0&code=" + access + "&country_code=CN&device_id=2C8B4939-0CCD-4E94-8CBA-CB8EA6E613A1&device_model=phone&grant_type=access_token&third_name=huami_phone")
                .execute()) {
            log.info("login res = {}", loginRes);
            access = loginRes.body();
        }
        return access;
    }

    public String submitStep(int targetSteps, String userId, String appToken) {
        String data = "%5B%7B%22data_hr%22%3A%22%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F9L%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F";
        data = data + "%5C%2F%5C%2F%5C%2F%5C%2FVv%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F0v%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F";
        data = data + "%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F9e%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F0n%5C%2Fa%5C%2F%5C%2F%5C%2FS%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F0b%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F1FK%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F";
        data = data + "%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2FR%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F9PTFFpaf9L%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2FR%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F0j%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F9K%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2FOv%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2Fzf%5C%2F%5C";
        data = data + "%2F%5C%2F86%5C%2Fzr%5C%2FOv88%5C%2Fzf%5C%2FPf%5C%2F%5C%2F%5C%2F0v%5C%2FS%5C%2F8%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2FSf%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2Fz3%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F0r%5C%2FOv%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2FS%5C%2F9L%5C%2Fzb%5C%2FSf9K%5C%2F0v%5C%2FRf9H%5C%2Fzj%5C%2FSf9K%5C%2F0%5C%2F%5C%2FN";
        data = data + "%5C%2F%5C%2F%5C%2F%5C%2F0D%5C%2FSf83%5C%2Fzr%5C%2FPf9M%5C%2F0v%5C%2FOv9e%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F";
        data = data + "%5C%2F%5C%2FS%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2Fzv%5C%2F%5C%2Fz7%5C%2FO%5C%2F83%5C%2Fzv%5C%2FN";
        data = data + "%5C%2F83%5C%2Fzr%5C%2FN%5C%2F86%5C%2Fz%5C%2F%5C%2FNv83%5C%2Fzn%5C%2FXv84%5C%2Fzr%5C%2FPP84%5C%2Fzj%5C%2FN%5C%2F9e%5C%2Fzr%5C%2FN%5C";
        data = data + "%2F89%5C%2F03%5C%2FP%5C%2F89%5C%2Fz3%5C%2FQ%5C%2F9N%5C%2F0v%5C%2FTv9C%5C%2F0H%5C%2FOf9D%5C%2Fzz%5C%2FOf88%5C%2Fz%5C%2F%5C%2FPP9A%5C";
        data = data + "%2Fzr%5C%2FN%5C%2F86%5C%2Fzz%5C%2FNv87%5C%2F0D%5C%2FOv84%5C%2F0v%5C%2FO%5C%2F84%5C%2Fzf%5C%2FMP83%5C%2FzH%5C%2FNv83%5C%2Fzf%5C%2FN";
        data = data + "%5C%2F84%5C%2Fzf%5C%2FOf82%5C%2Fzf%5C%2FOP83%5C%2Fzb%5C%2FMv81%5C%2FzX%5C%2FR%5C%2F9L%5C%2F0v%5C%2FO%5C%2F9I%5C%2F0T%5C%2FS%5C%2F9A";
        data = data + "%5C%2Fzn%5C%2FPf89%5C%2Fzn%5C%2FNf9K%5C%2F07%5C%2FN%5C%2F83%5C%2Fzn%5C%2FNv83%5C%2Fzv%5C%2FO%5C%2F9A%5C%2F0H%5C%2FOf8%5C%2F%5C%2Fzj";
        data = data + "%5C%2FPP83%5C%2Fzj%5C%2FS%5C%2F87%5C%2Fzj%5C%2FNv84%5C%2Fzf%5C%2FOf83%5C%2Fzf%5C%2FOf83%5C%2Fzb%5C%2FNv9L%5C%2Fzj%5C%2FNv82%5C%2Fzb";
        data = data + "%5C%2FN%5C%2F85%5C%2Fzf%5C%2FN%5C%2F9J%5C%2Fzf%5C%2FNv83%5C%2Fzj%5C%2FNv84%5C%2F0r%5C%2FSv83%5C%2Fzf%5C%2FMP%5C%2F%5C%2F%5C%2Fzb%5C";
        data = data + "%2FMv82%5C%2Fzb%5C%2FOf85%5C%2Fz7%5C%2FNv8%5C%2F%5C%2F0r%5C%2FS%5C%2F85%5C%2F0H%5C%2FQP9B%5C%2F0D%5C%2FNf89%5C%2Fzj%5C%2FOv83%5C%2Fzv";
        data = data + "%5C%2FNv8%5C%2F%5C%2F0f%5C%2FSv9O%5C%2F0ZeXv%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F1X%5C%2F%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F9B%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2FTP%5C%2F%5C%2F";
        data = data + "%5C%2F1b%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F0%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F9N%5C%2F%5C%2F%5C";
        data = data + "%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2F%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C";
        data = data + "%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%5C%2Fv7%2B%22";
        data = data + "%2C%22date%22%3A%22" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "%22%2C%22data%22%3A%5B%7B%22start%22%3A0%2C%22stop%22%3A1439%2C%22value%22%3A%22UA8AUBQAUAwAUBoAUAEAYCcA";
        data = data + "UBkAUB4AUBgAUCAAUAEAUBkAUAwAYAsAYB8AYB0AYBgAYCoAYBgAYB4AUCcAUBsAUB8AUBwAUBIAYBkAYB8AUBoAUBMAUCEAUCIAYBYAUBwAUCAAUBgAUCAAUBcAYBsAYCUAA";
        data = data + "TIPYD0KECQAYDMAYB0AYAsAYCAAYDwAYCIAYB0AYBcAYCQAYB0AYBAAYCMAYAoAYCIAYCEAYCYAYBsAYBUAYAYAYCIAYCMAUB0AUCAAUBYAUCoAUBEAUC8AUB0AUBYAUDMAUD";
        data = data + "oAUBkAUC0AUBQAUBwAUA0AUBsAUAoAUCEAUBYAUAwAUB4AUAwAUCcAUCYAUCwKYDUAAUUlEC8IYEMAYEgAYDoAYBAAUAMAUBkAWgAAWgAAWgAAWgAAWgAAUAgAWgAAUBAAUAQ";
        data = data + "AUA4AUA8AUAkAUAIAUAYAUAcAUAIAWgAAUAQAUAkAUAEAUBkAUCUAWgAAUAYAUBEAWgAAUBYAWgAAUAYAWgAAWgAAWgAAWgAAUBcAUAcAWgAAUBUAUAoAUAIAWgAAUAQAUAYA";
        data = data + "UCgAWgAAUAgAWgAAWgAAUAwAWwAAXCMAUBQAWwAAUAIAWgAAWgAAWgAAWgAAWgAAWgAAWgAAWgAAWREAWQIAUAMAWSEAUDoAUDIAUB8AUCEAUC4AXB4AUA4AWgAAUBIAUA8AUB";
        data = data + "AAUCUAUCIAUAMAUAEAUAsAUAMAUCwAUBYAWgAAWgAAWgAAWgAAWgAAWgAAUAYAWgAAWgAAWgAAUAYAWwAAWgAAUAYAXAQAUAMAUBsAUBcAUCAAWwAAWgAAWgAAWgAAWgAAUBgA";
        data = data + "UB4AWgAAUAcAUAwAWQIAWQkAUAEAUAIAWgAAUAoAWgAAUAYAUB0AWgAAWgAAUAkAWgAAWSwAUBIAWgAAUC4AWSYAWgAAUAYAUAoAUAkAUAIAUAcAWgAAUAEAUBEAUBgAUBcAWR";
        data = data + "YAUA0AWSgAUB4AUDQAUBoAXA4AUA8AUBwAUA8AUA4AUA4AWgAAUAIAUCMAWgAAUCwAUBgAUAYAUAAAUAAAUAAAUAAAUAAAUAAAUAAAUAAAUAAAWwAAUAAAcAAAcAAAcAAAcAAA";
        data = data + "cAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAeSEAeQ8AcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcA";
        data = data + "AAcAAAcBcAcAAAcAAAcCYOcBUAUAAAUAAAUAAAUAAAUAUAUAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcCgAeQAAcAAAcAAAcAAAcAAA";
        data = data + "cAAAcAYAcAAAcBgAeQAAcAAAcAAAegAAegAAcAAAcAcAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcCkAeQAAcAcAcAAAcAAAcAwAcAAAcA";
        data = data + "AAcAIAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcCIAeQAAcAAAcAAAcAAAcAAAcAAAeRwAeQAAWgAAUAAAUAAAUAAAUAAA";
        data = data + "UAAAcAAAcAAAcBoAeScAeQAAegAAcBkAeQAAUAAAUAAAUAAAUAAAUAAAUAAAcAAAcAAAcAAAcAAAcAAAcAAAegAAegAAcAAAcAAAcBgAeQAAcAAAcAAAcAAAcAAAcAAAcAkAeg";
        data = data + "AAegAAcAcAcAAAcAcAcAAAcAAAcAAAcAAAcA8AeQAAcAAAcAAAeRQAcAwAUAAAUAAAUAAAUAAAUAAAUAAAcAAAcBEAcA0AcAAAWQsAUAAAUAAAUAAAUAAAUAAAcAAAcAoAcAAAc";
        data = data + "AAAcAAAcAAAcAAAcAAAcAAAcAYAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcBYAegAAcAAAcAAAegAAcAcAcAAAcAAAcAAAcAAAcAAAeRkAegAAegAAcAAAcAAAcAAAcAAAcAA";
        data = data + "AcAAAcAAAcAEAcAAAcAAAcAAAcAUAcAQAcAAAcBIAeQAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcBsAcAAAcAAAcBcAeQAAUAAAUAAAUAAAUAAAUAAAUB";
        data = data + "QAcBYAUAAAUAAAUAoAWRYAWTQAWQAAUAAAUAAAUAAAcAAAcAAAcAAAcAAAcAAAcAMAcAAAcAQAcAAAcAAAcAAAcDMAeSIAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAc";
        data = data + "AAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcAAAcBQAeQwAcAAAcAAAcAAAcAMAcAAAeSoAcA8AcDMAcAYAeQoAcAwAcFQAcEMAeVIAaTYAbBcNYAsAYBIAYAIAYAIAYBUAYCwAYBM";
        data = data + "AYDYAYCkAYDcAUCoAUCcAUAUAUBAAWgAAYBoAYBcAYCgAUAMAUAYAUBYAUA4AUBgAUAgAUAgAUAsAUAsAUA4AUAMAUAYAUAQAUBIAASsSUDAAUDAAUBAAYAYAUBAAUAUAUCAAUB";
        data = data + "oAUCAAUBAAUAoAYAIAUAQAUAgAUCcAUAsAUCIAUCUAUAoAUA4AUB8AUBkAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAf";
        data = data + "gAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfg";
        data = data + "AAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAf";
        data = data + "gAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfg";
        data = data + "AAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAf";
        data = data + "gAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgA";
        data = data + "AfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfg";
        data = data + "AAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAf";
        data = data + "gAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgA";
        data = data + "AfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgA";
        data = data + "AfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfg";
        data = data + "AAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAf";
        data = data + "gAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA";
        data = data + "fgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgA";
        data = data + "AfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfg";
        data = data + "AAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAAfgAA%22%2C%22tz%22%3A32%2C%22did%22%3A%22DA932FFFFE8816E7";
        data = data + "%22%2C%22src%22%3A24%7D%5D%2C%22summary%22%3A%22%7B%5C%22v%5C%22%3A6%2C%5C%22slp%5C%22%3A%7B%5C%22st%5C%22%3A1628296479%2C%5C%22ed%5C%2";
        data = data + "2%3A1628296479%2C%5C%22dp%5C%22%3A0%2C%5C%22lt%5C%22%3A0%2C%5C%22wk%5C%22%3A0%2C%5C%22usrSt%5C%22%3A-1440%2C%5C%22usrEd%5C%22%3A-1440%2";
        data = data + "C%5C%22wc%5C%22%3A0%2C%5C%22is%5C%22%3A0%2C%5C%22lb%5C%22%3A0%2C%5C%22to%5C%22%3A0%2C%5C%22dt%5C%22%3A0%2C%5C%22rhr%5C%22%3A0%2C%5C%22s";
        data = data + "s%5C%22%3A0%7D%2C%5C%22stp%5C%22%3A%7B%5C%22ttl%5C%22%3A" + targetSteps + "%2C%5C%22dis%5C%22%3A10627%2C%5C%22cal%5C%22%3A510%2C%5C%22wk%5C%22%3A41%2C";
        data = data + "%5C%22rn%5C%22%3A50%2C%5C%22runDist%5C%22%3A7654%2C%5C%22runCal%5C%22%3A397%2C%5C%22stage%5C%22%3A%5B%7B%5C%22start%5C%22%3A327%2C%5C%22";
        data = data + "stop%5C%22%3A341%2C%5C%22mode%5C%22%3A1%2C%5C%22dis%5C%22%3A481%2C%5C%22cal%5C%22%3A13%2C%5C%22step%5C%22%3A680%7D%2C%7B%5C%22start%5C%2";
        data = data + "2%3A342%2C%5C%22stop%5C%22%3A367%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A2295%2C%5C%22cal%5C%22%3A95%2C%5C%22step%5C%22%3A2874%7D%2C%";
        data = data + "7B%5C%22start%5C%22%3A368%2C%5C%22stop%5C%22%3A377%2C%5C%22mode%5C%22%3A4%2C%5C%22dis%5C%22%3A1592%2C%5C%22cal%5C%22%3A88%2C%5C%22step%5";
        data = data + "C%22%3A1664%7D%2C%7B%5C%22start%5C%22%3A378%2C%5C%22stop%5C%22%3A386%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A1072%2C%5C%22cal%5C%22%3";
        data = data + "A51%2C%5C%22step%5C%22%3A1245%7D%2C%7B%5C%22start%5C%22%3A387%2C%5C%22stop%5C%22%3A393%2C%5C%22mode%5C%22%3A4%2C%5C%22dis%5C%22%3A1036%";
        data = data + "2C%5C%22cal%5C%22%3A57%2C%5C%22step%5C%22%3A1124%7D%2C%7B%5C%22start%5C%22%3A394%2C%5C%22stop%5C%22%3A398%2C%5C%22mode%5C%22%3A3%2C%5C%2";
        data = data + "2dis%5C%22%3A488%2C%5C%22cal%5C%22%3A19%2C%5C%22step%5C%22%3A607%7D%2C%7B%5C%22start%5C%22%3A399%2C%5C%22stop%5C%22%3A414%2C%5C%22mode%5";
        data = data + "C%22%3A4%2C%5C%22dis%5C%22%3A2220%2C%5C%22cal%5C%22%3A120%2C%5C%22step%5C%22%3A2371%7D%2C%7B%5C%22start%5C%22%3A415%2C%5C%22stop%5C%22%3";
        data = data + "A427%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A1268%2C%5C%22cal%5C%22%3A59%2C%5C%22step%5C%22%3A1489%7D%2C%7B%5C%22start%5C%22%3A428%2C";
        data = data + "%5C%22stop%5C%22%3A433%2C%5C%22mode%5C%22%3A1%2C%5C%22dis%5C%22%3A152%2C%5C%22cal%5C%22%3A4%2C%5C%22step%5C%22%3A238%7D%2C%7B%5C%22start";
        data = data + "%5C%22%3A434%2C%5C%22stop%5C%22%3A444%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A2295%2C%5C%22cal%5C%22%3A95%2C%5C%22step%5C%22%3A2874%7";
        data = data + "D%2C%7B%5C%22start%5C%22%3A445%2C%5C%22stop%5C%22%3A455%2C%5C%22mode%5C%22%3A4%2C%5C%22dis%5C%22%3A1592%2C%5C%22cal%5C%22%3A88%2C%5C%22s";
        data = data + "tep%5C%22%3A1664%7D%2C%7B%5C%22start%5C%22%3A456%2C%5C%22stop%5C%22%3A466%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A1072%2C%5C%22cal%5";
        data = data + "C%22%3A51%2C%5C%22step%5C%22%3A1245%7D%2C%7B%5C%22start%5C%22%3A467%2C%5C%22stop%5C%22%3A477%2C%5C%22mode%5C%22%3A4%2C%5C%22dis%5C%22%3A1";
        data = data + "036%2C%5C%22cal%5C%22%3A57%2C%5C%22step%5C%22%3A1124%7D%2C%7B%5C%22start%5C%22%3A478%2C%5C%22stop%5C%22%3A488%2C%5C%22mode%5C%22%3A3%2C%5";
        data = data + "C%22dis%5C%22%3A488%2C%5C%22cal%5C%22%3A19%2C%5C%22step%5C%22%3A607%7D%2C%7B%5C%22start%5C%22%3A489%2C%5C%22stop%5C%22%3A499%2C%5C%22mode";
        data = data + "%5C%22%3A4%2C%5C%22dis%5C%22%3A2220%2C%5C%22cal%5C%22%3A120%2C%5C%22step%5C%22%3A2371%7D%2C%7B%5C%22start%5C%22%3A500%2C%5C%22stop%5C%22%";
        data = data + "3A511%2C%5C%22mode%5C%22%3A3%2C%5C%22dis%5C%22%3A1268%2C%5C%22cal%5C%22%3A59%2C%5C%22step%5C%22%3A1489%7D%2C%7B%5C%22start%5C%22%3A512%2C";
        data = data + "%5C%22stop%5C%22%3A522%2C%5C%22mode%5C%22%3A1%2C%5C%22dis%5C%22%3A152%2C%5C%22cal%5C%22%3A4%2C%5C%22step%5C%22%3A238%7D%5D%7D%2C%5C%22goa";
        data = data + "l%5C%22%3A8000%2C%5C%22tz%5C%22%3A%5C%2228800%5C%22%7D%22%2C%22source%22%3A24%2C%22type%22%3A0%7D%5D";

        String url = "https://api-mifit-cn.huami.com/v1/data/band_data.json?&t=" + getTimeStamp();
        String str = "userid=" + userId + "&last_sync_data_time=1597306380&device_type=0&last_deviceid=DA932FFFFE8816E7&data_json=" + data;

        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        header.put("User-Agent", "MiFit/4.6.0 (iPhone; iOS 14.0.1; Scale/2.00)");
        header.put("apptoken", appToken);
        try (HttpResponse res = HttpUtil.createPost(url)
                .addHeaders(header)
                .body(str).execute()) {
            log.info("提交步数结果：{}", res);
            return res.body();
        }
    }

    private String getTimeStamp() {
        String res = HttpUtil.get("http://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp");
        return JSON.parseObject(res).getJSONObject("data").getString("t");
    }

    public void writeToken2(String appToken, String userId) {
        try (FileWriter fw = new FileWriter(tokenFilePath + "2");
             //写入中文字符时会出现乱码
             BufferedWriter bw = new BufferedWriter(fw)
        ) {
            String appTokenZip = ZipUtil.gzipB64(appToken);
            bw.write(appTokenZip == null ? "" : appTokenZip + System.getProperty("line.separator"));
            String userIdZip = ZipUtil.gzipB64(userId);
            bw.write(userIdZip == null ? "" : userIdZip);
        } catch (Exception e) {
            log.info("==============【写入token失败】=========,{}", e.toString());
            e.printStackTrace();
        }
    }

    public void writeToken(String accessToken, String userId) {
        try (FileWriter fw = new FileWriter(new File(tokenFilePath));
             //写入中文字符时会出现乱码
             BufferedWriter bw = new BufferedWriter(fw);
        ) {
            //BufferedWriter bw=new BufferedWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("temp.txt")), "UTF-8")));
            String accessTokenZip = ZipUtil.gzipB64(accessToken);
            bw.write(accessTokenZip == null ? "" : accessTokenZip + System.getProperty("line.separator"));
            String userIdZip = ZipUtil.gzipB64(userId);
            bw.write(userIdZip == null ? "" : userIdZip);
        } catch (Exception e) {
            log.info("==============【写入token失败】=========,{}", e.toString());
            e.printStackTrace();
        }
    }

    public Map<String, String> readToken() {
        Map<String, String> resultMap = new HashMap<>();
        try (FileReader fr = new FileReader(tokenFilePath);
             BufferedReader br = new BufferedReader(fr);
        ) {
            resultMap.put("accessToken", ZipUtil.unGzipB64(br.readLine()));
            resultMap.put("userId", ZipUtil.unGzipB64(br.readLine()));
        } catch (Exception e) {
            log.info("==============【读取token失败】=========,{}", e.toString());
            e.printStackTrace();
        }
        return resultMap;
    }

    public Map<String, String> readToken2() {
        Map<String, String> resultMap = new HashMap<>();
        try (FileReader fr = new FileReader(tokenFilePath + "2");
             BufferedReader br = new BufferedReader(fr);
        ) {
            resultMap.put("appToken", ZipUtil.unGzipB64(br.readLine()));
            resultMap.put("userId", ZipUtil.unGzipB64(br.readLine()));
        } catch (Exception e) {
            log.info("==============【读取token失败】=========,{}", e.toString());
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * post请求
     *
     * @param httpUrl httpUrl
     * @param param   param
     * @param header  header
     * @return 结果
     */
    private String doPost(String httpUrl, String param, Map<String, String> header) {
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        BufferedReader br = null;
        String result = null;
        try {
            URL url = new URL(httpUrl);
            // 通过远程url连接对象打开连接
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接请求方式
            connection.setRequestMethod("POST");
            // 设置连接主机服务器超时时间：15000毫秒
            connection.setConnectTimeout(15000);
            // 设置读取主机服务器返回数据超时时间：60000毫秒
            connection.setReadTimeout(60000);

            // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
            connection.setDoOutput(true);
            // 默认值为：true，当前向远程服务读取数据时，设置为true，该参数可有可无
            connection.setDoInput(true);
            // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
//            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (header != null && header.size() > 0) {
                for (String key : header.keySet()) {
                    connection.setRequestProperty(key, header.get(key));
                }
            }
            // 通过连接对象获取一个输出流
            os = connection.getOutputStream();
            // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的
            os.write(param.getBytes());
            // 通过连接对象获取一个输入流，向远程读取
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                // 对输入流对象进行包装:charset根据工作项目组的要求来设置
                br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sbf = new StringBuilder();
                String temp;
                // 循环遍历一行一行读取数据
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            ResourceUtil.closeResource(br);
            ResourceUtil.closeResource(os);
            ResourceUtil.closeResource(is);
            // 断开与远程地址url的连接
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }
}
