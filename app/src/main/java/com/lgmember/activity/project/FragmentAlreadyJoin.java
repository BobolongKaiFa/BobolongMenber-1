package com.lgmember.activity.project;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ikidou.fragmentBackHandler.BackHandlerHelper;
import com.github.ikidou.fragmentBackHandler.FragmentBackHandler;
import com.lgmember.activity.BaseFragment;
import com.lgmember.activity.MainActivity;
import com.lgmember.activity.R;
import com.lgmember.adapter.ProjectMessageListAdapter;
import com.lgmember.bean.ProjectMessageBean;
import com.lgmember.bean.TagsListResultBean;
import com.lgmember.business.project.ProjectMessageListBusiness;
import com.lgmember.business.project.TagListBusiness;
import com.lgmember.model.ProjectMessage;
import com.lgmember.model.Tag;
import com.lgmember.util.DataLargeHolder;
import com.lgmember.view.TopBarView;

import java.util.ArrayList;
import java.util.List;

import me.hwang.widgets.SmartPullableLayout;

public class FragmentAlreadyJoin extends BaseFragment implements ProjectMessageListBusiness.ProjectMessageListResultHandler,TopBarView.onTitleBarClickListener,FragmentBackHandler,TagListBusiness.TagListResultHandler{

	private TopBarView topBar;
	private LinearLayout ll_loading;
	private ProgressBar progressBar;
	private TextView loadDesc;
	private ListView lv_alread_join_list;
	private int pageNo = 1;
	private int pageSize = 5;
	private int total;
	private boolean isLoading;
	private int tab = 1;
	private int tagNum = 0;
	private List<ProjectMessage> projectMessageAlreadJoinList;
	private ProjectMessageListAdapter adapter;
	private String TAG = "-FragmentAlreadyJoin-";
	private SmartPullableLayout mPullableLayout;

	private RecyclerView rc_tags_list;
	private TagsListHorizontalAdapter tagsListAdapter;
	private List<Tag> tagList;

	private static final int ON_REFRESH = 1;
	private static final int ON_LOAD_MORE = 2;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ON_REFRESH:
					adapter.notifyDataSetChanged();
					mPullableLayout.stopPullBehavior();
					break;
				case ON_LOAD_MORE:
					mPullableLayout.stopPullBehavior();
					break;
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_activitylist, container, false);
		init(view);
		return view;
		}

	@Override
	public void onResume() {
		super.onResume();
		pageNo = 1;
		projectMessageAlreadJoinList.clear();
		lv_alread_join_list.setEnabled(false);
		getData();
	}

	private void init(View view) {
		tagList = new ArrayList<>();
		//获取标签列表数据
		rc_tags_list = (RecyclerView)view.findViewById(R.id.rc_tags_list);
		getTagsList();
		LinearLayoutManager layoutManager =new LinearLayoutManager(getContext());
		layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		rc_tags_list.setLayoutManager(layoutManager);
		tagsListAdapter = new TagsListHorizontalAdapter(tagList);
		rc_tags_list.setAdapter(tagsListAdapter);

		topBar = (TopBarView)view.findViewById(R.id.topbar);
		topBar.setClickListener(this);
		lv_alread_join_list=(ListView)
				view.findViewById(R.id.lv_all_activity_list);
		projectMessageAlreadJoinList = new ArrayList<>();
		adapter = new ProjectMessageListAdapter(getActivity(),projectMessageAlreadJoinList);
		lv_alread_join_list.setAdapter(adapter);
		lv_alread_join_list.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent,
									View view, int position, long id) {
				adapter.setCurrentItem(position);
				adapter.setClick(true);
				adapter.notifyDataSetChanged();
				ProjectMessage projectMessage =
						projectMessageAlreadJoinList.get(position);
				DataLargeHolder.getInstance()
						.save(projectMessage.getId(),projectMessage);
				Intent intent = new
						Intent(getActivity(),
						ProjectMessageDetailActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("id",projectMessage.getId());
				intent.putExtras(bundle);
				startActivity(intent);

			}
		});

		lv_alread_join_list.setOnScrollListener(new AbsListView.OnScrollListener() {
			//滑动状态改变的时候，回调
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
			//在滑动的时候不断的回调
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
								 int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem+visibleItemCount!=totalItemCount&&!isLoading) {
					isLoading = true;
					if (totalItemCount< total){
						pageNo++;
						getData();
					}
				}
			}
		});
		lv_alread_join_list.setOnScrollListener(new AbsListView.OnScrollListener() {
			//滑动状态改变的时候，回调
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			//在滑动的时候不断的回调
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
								 int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem+visibleItemCount==totalItemCount&&!isLoading) {
					isLoading = true;
					if (totalItemCount< total){
						pageNo++;
						getData();
					}
				}
			}
		});
		ll_loading = (LinearLayout)view.findViewById(R.id.ll_loading);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
		loadDesc = (TextView)view.findViewById(R.id.tv_loading_desc);
		mPullableLayout = (SmartPullableLayout)view.findViewById(R.id.layout_pullable);
		mPullableLayout.setOnPullListener(new SmartPullableLayout.OnPullListener() {
			@Override
			public void onPullDown() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							pageNo = 1;
							projectMessageAlreadJoinList.clear();
							getData();
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
							mHandler.sendEmptyMessage(ON_REFRESH);
					}
				}).start();
			}

			@Override
			public void onPullUp() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mHandler.sendEmptyMessage(ON_LOAD_MORE);
					}
				}).start();
			}
		});
	}
	private void getTagsList() {

		int tab = 1;
		TagListBusiness tagListBusiness = new TagListBusiness(getContext(),tab);
		tagListBusiness.setHandler(this);
		tagListBusiness.getAllTagList();

	}
	private void getData() {
		ProjectMessageListBusiness projectMessageListBusiness = new
				ProjectMessageListBusiness(getActivity(),pageNo,pageSize,tagNum);
		projectMessageListBusiness.setHandler(this);
		projectMessageListBusiness.getAlreadJoinList();
	}

	@Override
	public void onSuccess(final ProjectMessageBean bean) {
		total = bean.getTotal();
		if (bean.getList().size() == 0){
			lv_alread_join_list.setEnabled(false);
			progressBar.setVisibility(View.GONE);
			loadDesc.setText("还没有数据");
		}else {
			lv_alread_join_list.setEnabled(true);
			ll_loading.setVisibility(View.GONE);
			projectMessageAlreadJoinList.addAll(bean.getList());
			adapter.notifyDataSetChanged();
			isLoading = false;
		}
	}
	@Override
	public void onHotSuccess(ProjectMessageBean bean) {

	}

	@Override
	public void onBackClick() {
		Intent intent = new Intent(getActivity(), MainActivity.class);
		startActivity(intent);
	}

	@Override
	public void onRightClick() {

	}

	@Override
	public boolean onBackPressed() {
		// 当确认没有子Fragmnt时可以直接return false
		return BackHandlerHelper.handleBackPress(this);
	}

	@Override
	public void onTagListSuccess(TagsListResultBean tagsListResultBean) {

		Tag tag = new Tag();
		tag.setId(0);
		tag.setTag("ALL");
		tagList.add(tag);
		tagList.addAll(tagsListResultBean.getData());
		tagsListAdapter.notifyDataSetChanged();
	}
	class TagsListHorizontalAdapter extends RecyclerView.Adapter<TagsListHorizontalAdapter.ViewHolder> {

		private List<Tag> mTagsList ;
		int currentPosition = 0;
		class ViewHolder extends  RecyclerView.ViewHolder{
			LinearLayout ll_tag_item;
			TextView tv_tag_name;
			public ViewHolder(View view){
				super(view);
				ll_tag_item = (LinearLayout)view.findViewById(R.id.ll_tag_item);
				tv_tag_name = (TextView)view.findViewById(R.id.tv_tag_name);
			}
		}

		public TagsListHorizontalAdapter(List<Tag> tagList){
			mTagsList = tagList;

		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tags_recyclerview_horizontal,parent,false);
			final ViewHolder holder = new ViewHolder(view);
			holder.ll_tag_item.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int position = holder.getAdapterPosition();
					currentPosition = position;
					tagsListAdapter.notifyDataSetChanged();
					adapter.setCurrentItem(-1);
					adapter.setClick(true);
					adapter.notifyDataSetChanged();
					Tag tag = mTagsList.get(position);
					tagNum = tag.getId();
					projectMessageAlreadJoinList.clear();
					pageNo =  1;
					getData();
					adapter.notifyDataSetChanged();
				}
			});
			return holder;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			Tag tag = mTagsList.get(position);
			holder.tv_tag_name.setText(""+tag.getTag());
			if (currentPosition == position){
				holder.ll_tag_item.setBackgroundResource(R.drawable.tag_bg_press);
				holder.tv_tag_name.setTextColor(Color.WHITE);
			}else {
				holder.ll_tag_item.setBackgroundResource(R.drawable.tag_bg_nomal);
				holder.tv_tag_name.setTextColor(getActivity().getResources().getColor(R.color.main_2));
			}
		}

		@Override
		public int getItemCount() {
			return mTagsList.size();
		}
	}



}
