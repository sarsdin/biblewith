<?php

namespace Config;

use CodeIgniter\Config\BaseConfig;

/**
 * Enable/disable backward compatibility breaking features.
 */
class Feature extends BaseConfig
{
    /**
     * Enable multiple filters for a route or not.
     *
     * If you enable this:
     *   - CodeIgniter\CodeIgniter::handleRequest() uses:
     *     - CodeIgniter\Filters\Filters::enableFilters(), instead of enableFilter()
     *   - CodeIgniter\CodeIgniter::tryToRouteIt() uses:
     *     - CodeIgniter\Router\Router::getFilters(), instead of getFilter()
     *   - CodeIgniter\Router\Router::handle() uses:
     *     - property $filtersInfo, instead of $filterInfo
     *     - CodeIgniter\Router\RouteCollection::getFiltersForRoute(), instead of getFilterForRoute()
     *
     * @var bool
     */
    public $multipleFilters = false;

    /**
     * Use improved new auto routing instead of the default legacy version.
     */
    public bool $autoRoutesImproved = false;
//    public bool $autoRoutesImproved = true; //원래는 이걸 true 를 해야 향상된 오토라우팅이 되는데 지금 버그땜에 안되는듯 4.2.0
}
