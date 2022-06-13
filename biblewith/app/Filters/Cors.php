<?php

namespace App\Filters;

use CodeIgniter\Filters\FilterInterface;
use CodeIgniter\HTTP\RequestInterface;
use CodeIgniter\HTTP\ResponseInterface;

class Cors implements FilterInterface
{
    /**
     * Do whatever processing this filter needs to do.
     * By default it should not return anything during
     * normal execution. However, when an abnormal state
     * is found, it should return an instance of
     * CodeIgniter\HTTP\Response. If it does, script
     * execution will end and that Response will be
     * sent back to the client, allowing for error pages,
     * redirects, etc.
     *
     * @param RequestInterface $request
     * @param array|null       $arguments
     *
     * @return mixed
     */
    public function before(RequestInterface $request, $arguments = null)
    {
        //Cors 설정 부분 - 코드이그나이터4의 filter 기능을 이용해서 각 request 의 http 헤더에 추후 응답할때 어떤
        //리소스를 허용하는지에 대한 정보를 추가하여 준다.

        //(허용할) 들어오는 요청의 출처를 적는 곳. *은 모든 출처(요청한곳)에 이 서버의 리소스 접근을 허용한다는 뜻
        header("Access-Control-Allow-Origin: http://15.165.174.226:80"); //
        header("Access-Control-Allow-Credentials: true"); //cookie 허용인데 Origin 과는 같이 못쓰는 옵션
        //어떤 헤더들을 허용할지 명시하는 곳.
        header("Access-Control-Allow-Headers: X-API-KEY, Origin, X-Requested-With, Content-Type, Content-Length, Accept-Encoding, Accept, Access-Control-Requested-Method, Authorization, X-CSRF-Token, Cookie");
        header("Access-Control-Allow-Methods: GET, POST, OPTIONS, PATCH, PUT, DELETE"); //어떤 메소드를 허용할지 대한 곳곳
        $method = $_SERVER['REQUEST_METHOD'];
        if($method == "OPTIONS"){
            die();
            //post,get,head 등의 요청(simple request)가 아닌 일반요청은 요청전 사전 request란 것을 먼저 보내는데, 여기에 OPTIONS 메소드를 사용한다.
            //Access-Control-Allow-Headers, Access-Control-Allow-Methods, Origin 이 3가지를 판단하여 서버는 일반요청을 보내도 된다고 판단하면
            //200 ok 를 클라에 보내고 정상적인 일반요청이 이루어지게 된다.
        }
    }

    /**
     * Allows After filters to inspect and modify the response
     * object as needed. This method does not allow any way
     * to stop execution of other after filters, short of
     * throwing an Exception or Error.
     *
     * @param RequestInterface  $request
     * @param ResponseInterface $response
     * @param array|null        $arguments
     *
     * @return mixed
     */
    public function after(RequestInterface $request, ResponseInterface $response, $arguments = null)
    {
        //
    }
}
