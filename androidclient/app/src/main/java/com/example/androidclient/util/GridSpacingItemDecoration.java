package com.example.androidclient.util;



import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * 2단또는 단수가 높아지는 그리드 뷰의 경우 xml에서 아이템에 패딩을 설정해줄경우 좌측에 있는 아이템과 우측아이템의
 * 패딩이 동일하게 적용되어 중간의 패딩이 좌측 아이템 우측 아이템 합쳐진 패딩이 나타나게 된다
 * 그럴경우 xml에서 패딩을 조절 하지 말고
 * 코드에서 만약 우측 아이템일경우 padding-left를 빼주게 되면 여백이 골고루 들어가게 된다
 * */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;
    private boolean includeEdge;

    //단수,패딩값,
    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
           /* if(view instanceof ViewModel == false){ //홈 그리드 아이템이 아닌경우 패스
                return;
            }*/
        int position = parent.getChildAdapterPosition(view); // item position

        int spanIndex = ((StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();

        if (includeEdge) {


            if (spanIndex == 0) {
                //좌측 아이템이며 우측 패딩을 설정한 패딩의 1/2로 설정
                outRect.left = spacing;
                outRect.right = spacing/spanCount;
            } else {//if you just have 2 span . Or you can use (staggeredGridLayoutManager.getSpanCount()-1) as last span
                //우측 아이템이며 좌측 패딩을 설정한 패딩의 1/2로 설정
                outRect.left = spacing/spanCount;
                outRect.right = spacing;
            }

            //상단 패딩
            if (position < spanCount) { // top edge
                outRect.top = spacing;
            }

            //하단 패딩
            outRect.bottom = spacing; // item bottom
        }
    }
}


