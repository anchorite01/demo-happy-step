package com.example.demo.happy.step.task;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class StepTaskTest {

    @Resource
    private StepTask stepTask;

    @Test
    public void task2() throws InterruptedException {
        //        String loginRes = login2();
//        String loginRes = "{\"token_info\":{\"Klogin_token\":\"NQVBQFJyQktGHlp6QkpbRl5LRl5qek4uXAQEBAAAAADfIxbIV1wyK0sIdCdPvwcmTfFiQW65-9NJ0yZWuv39LQKR5-gA_i6zWtoV3xK6haHgUKXgUEG7nosQOdCd0pXlwGS8aoFOZPg8DEPBZFrJun5BU_G-XNvEQNmONJ1wZKhLFA01pDEZZUAi1QslobIxQgCj7UEL55EOG5UL5a8NASXk98csnetRNCy5QryCp2xZXbuHmOJ1kMy0Bh1ZCMOM\",\"app_token\":\"NQVBQFJyQktGHlp6QkpbRl5LRl5qek4uXAQABAAAAAHf5j856PvME0iYTP7V17xXlnix0jimLXiv6FbfarX6ffWGtmC9o7RnNzURL7L5_xLSY5f6iIz8VWohzwH66Py1La9DuUG2as23pqiYdNOoE_XdxCG58Z3zKln1hqPWaqULOWBnlTWz9i_zzg7SiJTtXbkssEZroBDMpZncmqlzZqIJeFsE4QjLxGTh8bY9zmQ\",\"user_id\":\"1097007255\",\"ttl\":31536000,\"app_ttl\":43200},\"regist_info\":{\"is_new_user\":0,\"regist_date\":1604113248456,\"region\":\"1\",\"country_code\":\"CN\"},\"thirdparty_info\":{\"nickname\":\"\",\"icon\":\"\",\"third_id\":\"NMPDvC5oV88H9XErl2fzvXAAAAXV8maxP\",\"email\":\"+8615090508234\"},\"result\":\"ok\",\"domain\":{\"id-dns\":\"https://account-cn2.huami.com\"}}";
//        JSONObject jsonObj = JSON.parseObject(loginRes);
//        JSONObject tokenInfo = jsonObj.getJSONObject("token_info");
//        String loginToken = tokenInfo.getString("login_token");
//        String userId = tokenInfo.getString("user_id");
//        String appToken = tokenInfo.getString("app_token");
//        System.out.println("logi nToken = " + loginToken);
//        System.out.println("userId = " + userId);
//        System.out.println("appToken = " + appToken);
//        System.out.println("login2() = " + loginRes);
////        stepTask.writeToken2(appToken, userId);
////        System.out.println("stepTask.readToken2() = " + stepTask.readToken2());
//
////        System.out.println("getTimeStamp() = " + getTimeStamp());
//        System.out.println("res=" + stepTask.submitStep(520, userId, appToken));
//        stepTask.task2();
    }

    @Test
    public void task() throws InterruptedException {
//        stepTask.task();
    }

    @Test
    public void writeReadToken() {
        String accessToken = "D2A6AFB93531605DBE56DC2EEE74C4C9C7B227AD040AB9F11531814553FC1853764048F2CE04BA69BB8EB6EA9DCAE73FA848D8C9A1BA671F2ABE5E7C60331D0BA1D6D795CCC4E4404BA7951A43C6181CDD8C7BE9FB1BDFA9CAE98A5AF8D0D766.48C068BC29B3A9BF60D43B0407CFF11B6ACB1BE88D1F717940099D8308CFC909";
        String userId = "27231098";
//        String compressAccessToken = ZipUtil.compress(accessToken);
//        System.out.println(compressAccessToken);
//        String compressUserId = ZipUtil.compress(userId);
//        System.out.println(compressUserId);
//
//        System.out.println(ZipUtil.uncompress(compressAccessToken));
//        System.out.println(ZipUtil.uncompress(compressUserId));

//        String gzipB64AccessToken = ZipUtil.gzipB64(accessToken);
//        System.out.println(gzipB64AccessToken);
//        String gzipB64UserId = ZipUtil.gzipB64(userId);
//        System.out.println(gzipB64UserId);
//        System.out.println(ZipUtil.uncompress(gzipB64AccessToken));
//        System.out.println(ZipUtil.uncompress(gzipB64UserId));
        stepTask.writeToken(accessToken, userId);
        System.out.println(stepTask.readToken());
    }
}