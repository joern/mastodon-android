package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.views.LinkedTextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ProfileAboutFragment extends Fragment{
	private static final int MAX_FIELDS=4;

	public UsableRecyclerView list;
	private List<AccountField> fields=Collections.emptyList();
	private AboutAdapter adapter;
	private Paint dividerPaint=new Paint();
	private boolean isInEditMode;
	private ItemTouchHelper dragHelper=new ItemTouchHelper(new ReorderCallback());
	private RecyclerView.ViewHolder draggedViewHolder;

	public void setFields(List<AccountField> fields){
		this.fields=fields;
		if(isInEditMode){
			isInEditMode=false;
			dragHelper.attachToRecyclerView(null);
		}
		if(adapter!=null)
			adapter.notifyDataSetChanged();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		list=new UsableRecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(adapter=new AboutAdapter());
		int pad=V.dp(16);
		list.setPadding(pad, pad, pad, pad);
		list.setClipToPadding(false);
		dividerPaint.setStyle(Paint.Style.STROKE);
		dividerPaint.setStrokeWidth(V.dp(1));
		dividerPaint.setColor(getResources().getColor(R.color.gray_200));
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				for(int i=0;i<parent.getChildCount();i++){
					View item=parent.getChildAt(i);
					int pos=parent.getChildAdapterPosition(item);
					int draggedPos=draggedViewHolder==null ? -1 : draggedViewHolder.getAbsoluteAdapterPosition();
					if(pos<adapter.getItemCount()-1 && pos!=draggedPos && pos!=draggedPos-1){
						c.drawLine(item.getLeft(), item.getBottom(), item.getRight(), item.getBottom(), dividerPaint);
					}
				}
			}
		});
		return list;
	}

	public void enterEditMode(List<AccountField> editableFields){
		isInEditMode=true;
		fields=editableFields;
		adapter.notifyDataSetChanged();
		dragHelper.attachToRecyclerView(list);
	}

	public List<AccountField> getFields(){
		return fields;
	}

	private class AboutAdapter extends UsableRecyclerView.Adapter<BaseViewHolder>{
		public AboutAdapter(){
			super(null);
		}

		@NonNull
		@Override
		public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new AboutViewHolder();
				case 1 -> new EditableAboutViewHolder();
				case 2 -> new AddRowViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(BaseViewHolder holder, int position){
			if(position<fields.size()){
				holder.bind(fields.get(position));
			}else{
				holder.bind(null);
			}
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			if(isInEditMode){
				int size=fields.size();
				if(size<MAX_FIELDS)
					size++;
				return size;
			}
			return fields.size();
		}

		@Override
		public int getItemViewType(int position){
			if(isInEditMode){
				return position==fields.size() ? 2 : 1;
			}
			return 0;
		}
	}

	private abstract class BaseViewHolder extends BindableViewHolder<AccountField>{
		private ShapeDrawable background=new ShapeDrawable();

		public BaseViewHolder(int layout){
			super(getActivity(), layout, list);
			background.getPaint().setColor(getResources().getColor(R.color.gray_50));
			itemView.setBackground(background);
		}

		@Override
		public void onBind(AccountField item){
			boolean first=getAbsoluteAdapterPosition()==0, last=getAbsoluteAdapterPosition()==adapter.getItemCount()-1;
			float radius=V.dp(10);
			float[] rad=new float[8];
			if(first)
				rad[0]=rad[1]=rad[2]=rad[3]=radius;
			if(last)
				rad[4]=rad[5]=rad[6]=rad[7]=radius;
			background.setShape(new RoundRectShape(rad, null, null));
			itemView.invalidateOutline();
		}
	}

	private class AboutViewHolder extends BaseViewHolder{
		private TextView title;
		private LinkedTextView value;

		public AboutViewHolder(){
			super(R.layout.item_profile_about);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
		}

		@Override
		public void onBind(AccountField item){
			super.onBind(item);
			title.setText(item.name);
			value.setText(item.parsedValue);
		}
	}

	private class EditableAboutViewHolder extends BaseViewHolder{
		private EditText title;
		private EditText value;

		public EditableAboutViewHolder(){
			super(R.layout.item_profile_about_editable);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			findViewById(R.id.dragger_thingy).setOnLongClickListener(v->{
				dragHelper.startDrag(this);
				return true;
			});
			title.addTextChangedListener(new SimpleTextWatcher(e->item.name=e.toString()));
			value.addTextChangedListener(new SimpleTextWatcher(e->item.value=e.toString()));
		}

		@Override
		public void onBind(AccountField item){
			super.onBind(item);
			title.setText(item.name);
			value.setText(item.value);
		}
	}

	private class AddRowViewHolder extends BaseViewHolder implements UsableRecyclerView.Clickable{
		public AddRowViewHolder(){
			super(R.layout.item_profile_about_add_row);
		}

		@Override
		public void onClick(){
			fields.add(new AccountField());
			if(fields.size()==MAX_FIELDS){ // replace this row with new row
				adapter.notifyItemChanged(fields.size()-1);
			}else{
				adapter.notifyItemInserted(fields.size()-1);
				rebind();
			}
		}
	}

	private class ReorderCallback extends ItemTouchHelper.SimpleCallback{
		public ReorderCallback(){
			super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target){
			if(target instanceof AddRowViewHolder)
				return false;
			int fromPosition=viewHolder.getAbsoluteAdapterPosition();
			int toPosition=target.getAbsoluteAdapterPosition();
			if (fromPosition<toPosition) {
				for (int i=fromPosition;i<toPosition;i++) {
					Collections.swap(fields, i, i+1);
				}
			} else {
				for (int i=fromPosition;i>toPosition;i--) {
					Collections.swap(fields, i, i-1);
				}
			}
			adapter.notifyItemMoved(fromPosition, toPosition);
			((BindableViewHolder)viewHolder).rebind();
			((BindableViewHolder)target).rebind();
			return true;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction){

		}

		@Override
		public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState){
			super.onSelectedChanged(viewHolder, actionState);
			if(actionState==ItemTouchHelper.ACTION_STATE_DRAG){
				viewHolder.itemView.setTag(R.id.item_touch_helper_previous_elevation, viewHolder.itemView.getElevation()); // prevents the default behavior of changing elevation in onDraw()
				viewHolder.itemView.animate().translationZ(V.dp(1)).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
				draggedViewHolder=viewHolder;
			}
		}

		@Override
		public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder){
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.animate().translationZ(0).setDuration(100).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
			draggedViewHolder=null;
		}

		@Override
		public boolean isLongPressDragEnabled(){
			return false;
		}
	}
}