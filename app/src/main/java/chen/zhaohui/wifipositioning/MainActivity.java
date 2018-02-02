package chen.zhaohui.wifipositioning;
/**
 * 开启后登陆界面Created by HP on 2017/5/2.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by 陈朝晖 on 2017/4/10.
 */

public class MainActivity extends AppCompatActivity {
   //JDBC设置
    private String driver = Config.DRIVER;
    private String url_pre = Config.URL_PRE;
    private String url_db = Config.URL_DB;
    private String url_mid = "";
    private String url,url1,url2;
    private String user = Config.USER;
    private String pass = Config.PASS;
    private String ip;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private EditText loginName;
    private EditText loginPassword;
    private EditText ipconfig_et;


    Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        //JDBC设置
        loginName = (EditText) findViewById(R.id.login_name_ET);
        loginPassword = (EditText) findViewById(R.id.login_password_ET);
        ipconfig_et = (EditText) findViewById(R.id.login_ip_ET);
        preferences = getSharedPreferences("ip", MODE_PRIVATE);


        login = (Button) findViewById(R.id.btnLogin);

    }

        public void loginDo(View v) {
            String name = loginName.getText().toString();
            String password = loginPassword.getText().toString();
            ip = ipconfig_et.getText().toString();
            if (ip.length() > 3) {
                editor = preferences.edit();
                editor.putString("ip", ip);
                editor.commit();
            }

            url_mid = preferences.getString("ip", "");
            url = url_pre + url_mid + url_db;                                      //离线定位数据库url
            Bundle data = new Bundle();
            data.putString("url", url);


                    //            跳转
                        Intent i = new Intent(MainActivity.this, chen.zhaohui.wifipositioning.MainMenu.class);
                        i.putExtras(data);
                        startActivity(i);


    }

}
