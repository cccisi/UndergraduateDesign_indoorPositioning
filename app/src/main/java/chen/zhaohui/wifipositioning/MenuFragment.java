package chen.zhaohui.wifipositioning;
/**
 *继承自ListFragment的，叫MenuFragment，在MainMenu中用到
 *  * Created by 陈朝晖 on 2017/5/2.

 */

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class MenuFragment extends ListFragment {

        private static final String labelMenuItem = "menu";
        private static final String labelMenuDesc = "desc";
        private static final String[] menuItems = {
                "调整平面图比例", "位置指纹采集", "实时在线定位"};
        private static final String[] menuDescs = {
                "在这里调节平面图单元格尺寸和比例尺",
                "在这里建立和完善WiFi指纹库，查询或删除位置指纹信息，第一次打开数据会从服务器同步到本机",
                "实时定位并且显示运动轨迹以及运动方向"};
        private static final String[] labelArray = {labelMenuItem, labelMenuDesc};
        private static final int[] labelIndex = {android.R.id.text1, android.R.id.text2};

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // prepare menu items
            ArrayList<HashMap<String,String>> menuList = new ArrayList<HashMap<String,String>>();
            for (int i=0; i<menuItems.length; i++) {
                HashMap<String,String> item = new HashMap<String,String>();
                item.put(labelMenuItem, menuItems[i]);
                item.put(labelMenuDesc, menuDescs[i]);
                menuList.add(item);
            }

            setListAdapter(new SimpleAdapter(getActivity(), menuList,
                    android.R.layout.simple_list_item_2, labelArray, labelIndex));
        }

        @Override
        public void onResume() {
            getActivity().getWindow().setTitle(MainMenu.TITLE_MAIN);
            super.onResume();
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            switch (position) {
                case 0:
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.container, new FloorPlanFragment(), MainMenu.TAG_FLOORPLAN_FRAGMENT);
                    ft.addToBackStack(null);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.commit();
                    return;
                case 1:
                    FragmentTransaction ft2 = getFragmentManager().beginTransaction();
                    ft2.replace(R.id.container, new SurveyFragment(), MainMenu.TAG_SURVEY_FRAGMENT);
                    ft2.addToBackStack(null);
                    ft2.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft2.commit();
                    return;
                case 2:
                    FragmentTransaction ft3 = getFragmentManager().beginTransaction();
                    ft3.replace(R.id.container, new PositioningFragment(), MainMenu.TAG_POSITIONING_FRAGMENT);
                    ft3.addToBackStack(null);
                    ft3.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft3.commit();
                    return;
                default:
                    Log.d("WiFiMenuFragment", "unknown menu item");
            }
        }
    }
