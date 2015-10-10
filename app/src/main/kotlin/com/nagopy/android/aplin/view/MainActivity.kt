package com.nagopy.android.aplin.view

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.google.android.gms.ads.AdView
import com.nagopy.android.aplin.Aplin
import com.nagopy.android.aplin.R
import com.nagopy.android.aplin.entity.AppEntity
import com.nagopy.android.aplin.model.Category
import com.nagopy.android.aplin.presenter.AdPresenter
import com.nagopy.android.aplin.presenter.MainScreenPresenter
import com.nagopy.android.aplin.view.adapter.MainScreenPagerAdapter
import javax.inject.Inject

/**
 * メインになる画面用のActivity
 */
public class MainActivity : AppCompatActivity(),
        MainScreenView
        , AppListViewParent // 子Viewから処理を移譲してもらうためのインターフェース
{

    val toolbar: Toolbar by lazy {
        findViewById(R.id.toolbar) as Toolbar
    }

    val tabLayout: TabLayout by lazy { findViewById(R.id.tab) as TabLayout }

    val viewPager: ViewPager by lazy { findViewById(R.id.pager) as ViewPager }

    val adView: AdView by lazy { findViewById(R.id.adView) as AdView }

    val progressBar: ProgressBar by lazy { findViewById(R.id.progress) as ProgressBar }

    @Inject
    lateinit var presenter: MainScreenPresenter

    @Inject
    lateinit var adPresenter: AdPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Aplin.getApplicationComponent().inject(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        presenter.initialize(this)
        adPresenter.initialize(adView)
    }

    override fun onResume() {
        super.onResume()
        presenter.resume()
        adPresenter.resume()
    }

    override fun onPause() {
        super.onPause()
        presenter.pause()
        adPresenter.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
        adPresenter.destroy()
    }

    override fun showIndicator() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideIndicator() {
        progressBar.visibility = View.GONE
    }

    override fun showAppList(categories: List<Category>) {
        viewPager.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE

        val adapter = MainScreenPagerAdapter(applicationContext, supportFragmentManager, categories)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun hideAppList() {
        viewPager.visibility = View.INVISIBLE
        tabLayout.visibility = View.INVISIBLE
    }

    // menu ===============================================================================================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem, appList: List<AppEntity>) {
        presenter.onMenuItemClicked(item, appList)
    }


    override fun onListItemClick(app: AppEntity) {
        presenter.listItemClicked(this, app)
    }

    override fun onListItemLongClick(app: AppEntity) {
        presenter.listItemLongClicked(app)
    }
}