package chen.zhaohui.wifipositioning;
/**
 * 主界面Created by HP on 2017/5/2.
 */

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainMenu extends Activity {
//implements SwipeRefreshLayout.OnRefreshListener
//    protected WifiData wData;

    public static final String TAG_MENU_FRAGMENT = "menu";
    public static final String TAG_FLOORPLAN_FRAGMENT = "floorplan";
    public static final String TAG_INFO_DIALOG_FRAGMENT = "info_dialog";
    public static final String TAG_SURVEY_FRAGMENT = "survey";
    public static final String TAG_SURVEY_SCAN_FRAGMENT = "survey_scan";
    public static final String TAG_SURVEY_LIST_FRAGMENT = "survey_list";
    public static final String TAG_SURVEY_DETAIL_FRAGMENT = "survey_detail";
    public static final String TAG_POSITIONING_FRAGMENT = "positioning";

    public static final String TITLE_MAIN = "WiFi Positioning";
    public static final String TITLE_FLOORPLAN = "Floor Plan";
    public static final String TITLE_SURVEY = "WiFi Surveying";
    public static final String TITLE_POSITIONING = "WiFi Positioning";

    protected long lastBackPressTime = 0;
    protected Toast toast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        MenuFragment menuFragment = new MenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.container, menuFragment, TAG_MENU_FRAGMENT);
        ft.commit();
    }


//    public void onRefresh() {
//        wData.reFresh();
//    }

    public boolean hasFragmentStack() {
        if (getFragmentManager().findFragmentByTag(TAG_FLOORPLAN_FRAGMENT) != null) {
            Log.d("MainActivity", "floorplan fragment found");
            return true;
        } else if (getFragmentManager().findFragmentByTag(TAG_SURVEY_FRAGMENT) != null) {
            Log.d("MainActivity", "survey fragment found");
            return true;
        } else if (getFragmentManager().findFragmentByTag(TAG_SURVEY_SCAN_FRAGMENT) != null) {
            Log.d("MainActivity", "survey_scan fragment found");
            return true;
        } else if (getFragmentManager().findFragmentByTag(TAG_SURVEY_LIST_FRAGMENT) != null) {
            Log.d("MainActivity", "survey_list fragment found");
            return true;
        } else if (getFragmentManager().findFragmentByTag(TAG_SURVEY_DETAIL_FRAGMENT) != null) {
            Log.d("MainActivity", "survey_detail fragment found");
            return true;
        } else if (getFragmentManager().findFragmentByTag(TAG_POSITIONING_FRAGMENT) != null) {
            Log.d("MainActivity", "positioning fragment found");
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!hasFragmentStack()) {
            if (lastBackPressTime < System.currentTimeMillis() - 4000) {
                toast = Toast.makeText(this, "真的要离开陈朝晖的APP吗！！！",
                        Toast.LENGTH_SHORT);
                toast.show();
                lastBackPressTime = System.currentTimeMillis();
            } else {
                // 4秒延时，若早、再次退出
                if (toast != null)
                    toast.cancel();
                Log.d("MainActivity", "quit app now");
                finish(); // quit the app
            }
        } else {
            super.onBackPressed();
        }
    }
}
