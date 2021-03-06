package com.anlooper.photosample.view.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.anlooper.photosample.R
import com.anlooper.photosample.adapter.PhotoAdapter
import com.anlooper.photosample.constant.Constant
import com.anlooper.photosample.constant.FlickrTypeConstant
import com.anlooper.photosample.data.Photo
import com.anlooper.photosample.network.FlickrModule
import com.anlooper.photosample.view.detail.DetailActivity
import com.anlooper.photosample.view.main.presenter.MainContract
import com.anlooper.photosample.view.main.presenter.MainPresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import tech.thdev.base.util.createBlurImage
import tech.thdev.base.view.BasePresenterFragment
import java.util.concurrent.TimeUnit

/**
 * Created by Tae-hwan on 7/21/16.
 */
class MainFragment : BasePresenterFragment<MainContract.View, MainContract.Presenter>(), MainContract.View {

    private var isLoading: Boolean = false
    private var page: Int = 0

    private var adapter: PhotoAdapter? = null

    private val recyclerView by lazy {
        view?.findViewById(R.id.recycler_view) as RecyclerView
    }

    private val clBlur by lazy {
        activity?.findViewById(R.id.constraintLayout) as ConstraintLayout
    }

    private val imgBlurBackground by lazy {
        activity?.findViewById(R.id.img_blur_background) as ImageView
    }

    private val imgView by lazy {
        activity?.findViewById(R.id.img_view) as ImageView
    }

    private val containerMain by lazy {
        activity?.findViewById(R.id.container_main) as CoordinatorLayout
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePresenter() = MainPresenter(FlickrModule())

    override fun getLayout() = R.layout.fragment_main

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotoAdapter(context)
        adapter?.setLongClickListener { baseRecyclerAdapter, i ->
            presenter?.updateLongClickItem(i) ?: false
        }

        adapter?.setOnClickListener(
                { baseRecyclerAdapter, i -> presenter?.loadDetailView(i) },
                { Toast.makeText(context, "Test", Toast.LENGTH_SHORT).show() })

        recyclerView.addOnScrollListener(InfiniteScrollListener({ presenter?.loadPhotos(page) },
                recyclerView.layoutManager as StaggeredGridLayoutManager))

        recyclerView.adapter = adapter

        presenter?.setAdapterModel(adapter)
        presenter?.setAdapterView(adapter)

        containerMain.isDrawingCacheEnabled = true

        initPhotoList()
    }

    override fun showDetailView(photo: Photo?) {
        val intent = Intent(context, DetailActivity::class.java)
        intent.putExtra(Constant.KEY_PHOTO_DATA, photo)
        startActivity(intent)
    }

    override fun showBlurDialog(imageUrl: String?) {
        clBlur.visibility = View.VISIBLE
        imgView.visibility = View.VISIBLE
        imgBlurBackground.visibility = View.VISIBLE

        drawBackgroundImage()

        Glide.with(context)
                .load(imageUrl)
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        /*
                         * Timer
                         */
                        Observable.timer(5, TimeUnit.SECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    clBlur.visibility = View.GONE
                                    imgView.setImageResource(0)
                                    imgView.visibility = View.GONE
                                    imgBlurBackground.setImageResource(0)
                                    imgBlurBackground.visibility = View.GONE
                                }
                        return false
                    }

                    override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                        clBlur.visibility = View.GONE
                        imgView.setImageResource(0)
                        imgView.visibility = View.GONE
                        imgBlurBackground.setImageResource(0)
                        imgBlurBackground.visibility = View.GONE

                        Toast.makeText(context, "Image load fail", Toast.LENGTH_SHORT).show()
                        return false
                    }
                })
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .crossFade()
                .into(imgView)
    }

    /**
     * Root capture...
     */
    private fun drawBackgroundImage() {
        containerMain.isDrawingCacheEnabled = true
        containerMain.buildDrawingCache(true)
        containerMain.drawingCache.createBlurImage(context)?.let {
            imgBlurBackground.setImageBitmap(it)
        }
        containerMain.isDrawingCacheEnabled = false
    }

    override fun showProgress() {
        isLoading = true
    }

    override fun hideProgress() {
        isLoading = false
    }

    override fun showFailLoad() {
        Toast.makeText(context, "FAIL", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter?.unSubscribeSearch()
        recyclerView.removeOnScrollListener(InfiniteScrollListener({
            presenter?.loadPhotos(page)
        }, recyclerView.layoutManager as StaggeredGridLayoutManager))
    }

    override fun initPhotoList() {
        page = 0
        presenter?.loadPhotos(page)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_search, menu)

        val searchView: SearchView = MenuItemCompat.getActionView(menu?.findItem(R.id.action_search)) as SearchView
        val searchManager: SearchManager? = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        page = 0
                        presenter?.searchPhotos(page, FlickrTypeConstant.TYPE_SAFE_SEARCH_SAFE, query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        page = 0
                        presenter?.searchPhotos(page, FlickrTypeConstant.TYPE_SAFE_SEARCH_SAFE, newText)
                        return true
                    }
                }
        )

        super.onCreateOptionsMenu(menu, inflater)
    }

    inner class InfiniteScrollListener(
            val func: () -> Unit,
            val layoutManager: StaggeredGridLayoutManager) : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val visibleItemCount = recyclerView?.childCount as Int
            val totalItemCount = adapter?.itemCount as Int
            var firstVisibleItem: IntArray? = null
            firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPositions(firstVisibleItem)

            var firstItemNumber = 0
            firstVisibleItem?.let {
                if (it.size > 0) {
                    firstItemNumber = it[0]
                }

                if (!isLoading && (firstItemNumber + visibleItemCount) >= totalItemCount - 10) {
                    ++page
                    func()
                }
            }
        }
    }
}
