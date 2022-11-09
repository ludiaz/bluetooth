package br.com.ludiaz.bluetooth.adapter;

import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import br.com.ludiaz.bluetooth.MainActivity;
import br.com.ludiaz.bluetooth.R;
import br.com.ludiaz.bluetooth.util.DeviceItem;

public class ListExpandable extends BaseExpandableListAdapter {

    private Context mContext;
    private List<String> mGroups;
    private HashMap<String, List<DeviceItem>> mDevicesGroups;

    private int selectedIndex = -1;

    public ListExpandable(Context context, List<String> groups, HashMap<String, List<DeviceItem>> devicesGroups){
        this.mContext = context;
        this.mGroups = groups;
        this.mDevicesGroups = devicesGroups;
    }

    @Override
    public int getGroupCount() {
        // retorna a quantidade de grupos
        return mGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // retorna a quantidade de itens de um grupo
        return mDevicesGroups.get(getGroup(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        // retorna um grupo
        return mGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        // retorna um item do grupo
        return mDevicesGroups.get(getGroup(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        // retorna o id do grupo, porém como nesse exemplo
        // o grupo não possui um id específico, o retorno
        // será o próprio groupPosition
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // retorna o id do item do grupo, porém como nesse exemplo
        // o item do grupo não possui um id específico, o retorno
        // será o próprio childPosition
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        // retorna se os ids são específicos (únicos para cada
        // grupo ou item) ou relativos
        return false;
    }

    private static class ViewHolderGroup{
        TextView name;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // cria os itens principais (grupos)

        ViewHolderGroup viewHolder;

        if (convertView == null) {
            //LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //convertView = layoutInflater.inflate(R.layout.device_grupo, null);

            LayoutInflater layoutInflater =  ((MainActivity)mContext).getLayoutInflater();
            convertView = layoutInflater.inflate(R.layout.device_grupo, parent, false);

            viewHolder = new ViewHolderGroup();
            viewHolder.name = convertView.findViewById(R.id.textViewName);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolderGroup) convertView.getTag();
        }

        String group = (String)getGroup(groupPosition);

        if(group != null){
            if(!group.equals("")){
                viewHolder.name.setText(group);
                Log.d("ListExpandable", "getGroupView: " + group);
            }else {
                Log.d("ListExpandable", "getGroupView: group is igual empty");
            }
        }else{
            Log.d("ListExpandable", "getGroupView: group is null");
        }

        return convertView;
    }

    private static class ViewHolderChild{
        TextView name;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        ViewHolderChild viewHolder;

        if(convertView == null){
            //LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //convertView = layoutInflater.inflate(R.layout.device_item, null);
            LayoutInflater layoutInflater =  ((MainActivity)mContext).getLayoutInflater();
            convertView = layoutInflater.inflate(R.layout.device_item, parent, false);

            viewHolder = new ViewHolderChild();

            viewHolder.name = convertView.findViewById(R.id.textViewName);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolderChild) convertView.getTag();
        }

        DeviceItem deviceItem = (DeviceItem)getChild(groupPosition, childPosition);

        if(deviceItem != null) {
            viewHolder.name.setText(deviceItem.getName() + " ("+ deviceItem.getAddress()+")");
            Log.d("ListExpandable", "getChildView: " + deviceItem.getName() + " ("+ deviceItem.getAddress()+")");
        }else{
            Log.d("ListExpandable", "getChildView: deviceItem is null");
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // retorna se o subitem (item do grupo) é selecionável
        return true;
    }

    public void setNewItem(int groupPosition, DeviceItem deviceItem){

        List<DeviceItem> deviceItems =  mDevicesGroups.get(mGroups.get(groupPosition));

        boolean canInsert = true;

        for (DeviceItem di : deviceItems){
            if(di.getAddress().equals(deviceItem.getAddress())){
                canInsert = false;
                break;
            }
        }

        if(canInsert) {
            mDevicesGroups.replace(mGroups.get(groupPosition), deviceItems);
        }

        notifyDataSetChanged();
    }
}
