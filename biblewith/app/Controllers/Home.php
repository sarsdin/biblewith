<?php

namespace App\Controllers;

use App\Models\Tempinfo;
use App\Models\User;
use CodeIgniter\Database\Exceptions\DataException;
use CodeIgniter\HTTP\ResponseInterface;
use Config\Pager;
use DateTime;
use DateTimeZone;

class Home extends BaseController
{

    public function index()
    {
        return view('welcome_message');
    }

    public function index2()
    {
        return phpinfo();
    }


    //회원 가입 처리
    public function join() : ResponseInterface
    {
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
//        log_message("debug", "join req: ". print_r($req, true));
//        log_message("debug", "request getjson: ". print_r($req->getJSON(true), true));
//        log_message("debug", "requesturi: ".$req->uri);
//        log_message("debug", "requestURI path: " . $req->getUri()->getPath());
//        log_message("info", "requestData.userid: ".$req->getJSON()->user_id);

        $data['user_create_date'] = date('Y-m-d H:i:s');
        log_message("debug", "[Home] join \$data: ". print_r($data, true));

        try {
            $result = $user->insert($data);
            log_message("debug", "[Home] join \$result: ". print_r($result, true));

            if ($result === false) {    //insert 작업이 실패할경우 false를 리턴하는데 이경우 users모델에 지정된 validation error메시지를 클라에 보낸다.
                return $res->setJSON([
                    'result' => $result,
                    'msg' => $user->errors()
                ]);

            } else {
                return $res->setJSON([
                    'result' => true,
                    'msg' => '회원가입 완료하였습니다. 가입한 아이디로 로그인해주세요.'
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //JoinFm 에서 이메일 중복 여부 체크
    public function isEmailRedundant() : ResponseInterface
    {
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
//        $data = $req->getJSON(true);
        log_message("debug", "[Home] isEmailRedundant \$data: ". print_r($data, true));
//        log_message("debug", "request getjson: ". print_r($req->getJSON(true), true));
//        log_message("debug", "requesturi: ".$req->uri);
//        log_message("debug", "requestURI path: " . $req->getUri()->getPath());
//        log_message("info", "requestData.userid: ".$req->getJSON()->user_id);

        try {
            $result = $user->where('user_email', $data['user_email'])->findColumn('user_email');

            if ($result === null) {
                return $res->setJSON([
                    'result' => true,
                    'msg' => '사용 가능한 이메일 입니다.',
                    'email' => $data['user_email']
                ]);

            } else {
                return $res->setJSON([
                    'result' => false,
                    'msg' => '중복된 이메일 입니다.',
                    'email' => $result
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    //FindPwFm에서 인증메일 발송 버튼 클릭시
    public function findpwMailSend() : ResponseInterface
    {
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Home] findpwMailSend \$data: ". print_r($data, true));

        try {
            $result = $user->where('user_email', $data['user_email'])
                ->where('user_name', $data['user_name'])->countAllResults();

            if ($result === 0) {
                return $res->setJSON([
                    'result' => false,
                    'msg' => '존재하지 않는 회원입니다.',
                    'email' => $data['user_email'],
                    'name' => $data['user_name'],
                ]);

            } else {
                //인증번호 발송처리
                $vnum = $this->난수생성();
                $email = \Config\Services::email();
                $email->setFrom('sjeys14@gmail.com', '성경with');
                $email->setTo($data['user_email']);
                $email->setSubject('성경with 비밀번호 찾기 인증번호: '.$vnum);
                $email->setMessage('인증번호는 '.$vnum. ' 입니다.');
                $email->send();
                //인증번호 Tempinfo테이블에 저장(email,name, 만료시간(현재시간+3분))
                $tempInfo = new Tempinfo();
                $tmpTime = new DateTime('+3 minutes', new DateTimeZone('Asia/Seoul'));
                $tmpArray = [
                    'email' => $data['user_email'],
                    'name' => $data['user_name'],
                    'vnum_expire_time' => $tmpTime->format('Y-m-d H:i:s'),
                    'findpw_number' => $vnum
                ];

                $isEmail = $tempInfo->where('email', $data['user_email'])->countAllResults();
                $resSave = null;
                if ($isEmail > 0) { //Tempinfo 테이블에 email이 존재하는지 확인 후 존재하면 update 아니면 insert
                    $resSave = $tempInfo->update($data['user_email'], $tmpArray);

                } else {
                    $resSave = $tempInfo->insert($tmpArray);
                }

                log_message("debug", "[Home] findpwMailSend \$tmpArray: ". print_r($tmpArray, true));
                log_message("debug", "[Home] findpwMailSend \$resSave: ". print_r($resSave, true));

                return $res->setJSON([
                    'result' => true,
                    'msg' => '인증 번호를 입력하신 이메일로 발송했습니다.',
                    'email' => $data['user_email']
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    //FindPwFm에서 인증번호 확인 클릭시
    public function findpwMailVnumConfirm() : ResponseInterface
    {
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Home] findpwMailVnumConfirm \$data: ". print_r($data, true));
        //인증번호 Tempinfo테이블에 저장(email,name, 만료시간(현재시간+3분))
        $tempInfo = new Tempinfo();

        try {
            //인증번호 만료시간 확인
            $tmpinfo = $tempInfo->find($data['email']);
            log_message("debug", "[Home] findpwMailVnumConfirm \$tmpinfo: ". print_r($tmpinfo, true));
            $expireT = date('Y-m-d H:i:s', strtotime($tmpinfo['vnum_expire_time'])); //문자열을 시간형식으로 변경
            $currentT = date('Y-m-d H:i:s');

            if ( $expireT < $currentT ) {
                return $res->setJSON([
                    'result' => false,
                    'msg' => '인증번호 시간이 만료되었습니다.',
                    'email' => $data['email']
                ]);
            }

            //인증번호 확인
            $result = $tempInfo->where('email', $data['email'])
                ->where('name', $data['name'])->findColumn('findpw_number'); //findColumn() 은 배열형식으로 반환한다.
            log_message("debug", "[Home] findpwMailVnumConfirm \$result: ". print_r($result, true));

            if ($result[0] == $data['findpw_number']) {
                return $res->setJSON([
                    'result' => true,
                    'msg' => '인증번호가 확인되었습니다.',
                    'email' => $data['email']
                ]);

            } else {
                return $res->setJSON([
                    'result' => false,
                    'msg' => '인증번호가 다릅니다.',
                    'email' => $data['email']
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //FindPwFm에서 새 비밀번호 변경완료 클릭시
    public function findpwNewPw() : ResponseInterface
    {
        $user = new User();
        $tempInfo = new Tempinfo();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Home] findpwNewPw \$data: ". print_r($data, true));

        try {
            //인증번호 만료시간 확인
            $result = $user->where('user_email', $data['user_email'])->set('user_pwd',$data['user_pwd'])->update();
            log_message("debug", "[Home] findpwNewPw \$result: ". print_r($result, true));

            if ($result == true) {
                return $res->setJSON([
                    'result' => true,
                    'msg' => '비밀번호가 변경되었습니다.',
                    'email' => $data['user_email']
                ]);

            } else {
                return $res->setJSON([
                    'result' => false,
                    'msg' => '비밀번호 변경 실패!',
                    'email' => $data['user_email']
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //loginmainFm에서 로그인 클릭시
    public function login() : ResponseInterface
    {
        $user = new User();
        $tempInfo = new Tempinfo();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Home] login \$data: ". print_r($data, true));

        try {
            $result = $user->where('user_email', $data['user_email'])->where('user_pwd',$data['user_pwd'])->find();
            log_message("debug", "[Home] login \$result: ". print_r($result, true));

            if ($result != null) { //로그인 정상적이라면

                return $res->setJSON($result[0]); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Home] login \$result is null: ". print_r($result, true));
                return $res->setJSON($result[0]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //자동로그인시 유저정보 요청
    public function getAutoLoginInfo() : ResponseInterface
    {
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Home] getAutoLoginInfo \$data: ". print_r($data, true));

        try {
            $result = $user->where('user_email', $data['user_email'])->find();
            log_message("debug", "[Home] getAutoLoginInfo \$result: ". print_r($result, true));

            if ($result != null) { //유저정보가 정상적이라면
                return $res->setJSON($result[0]); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Home] login \$result is null: ". print_r($result, true));
                return $res->setJSON($result[0]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }







    //4자리 숫자의 인증번호 생성
    public function 난수생성() : String
    {
        $res = [
            mt_rand(0, 9),
            mt_rand(0, 9),
            mt_rand(0, 9),
            mt_rand(0, 9)
        ];
        return implode($res);
    }







}
