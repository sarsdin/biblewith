<?php

namespace App\Controllers;

use App\Models\GboardLike;
use App\Models\GroupBoard;
use App\Models\GroupBoardImage;
use App\Models\GroupMember;
use App\Models\Note;
use App\Models\NoteVerse;
use App\Models\Reply;
use CodeIgniter\Database\Exceptions\DataException;
use CodeIgniter\HTTP\ResponseInterface;
use Config\Pager;

class Group extends \CodeIgniter\Controller
{

    // 모임 만들기 버튼 클릭시
    public function createGroup() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] createGroup \$data: ". print_r($data, true));
//        $imgData3 = $req->getFileMultiple('product_image');
//        log_message("debug", "[Group] createGroup \$getFileMultiple: ".print_r($imgData3, true));
        $imgData = $req->getFile('group_main_image');
//        $imgData = $req->getFiles();
        log_message("debug", "[Group] createGroup \$getFiles: ".print_r($imgData, true));
//        return $res->setJSON([
//            "result" => "test",
//            "msg" => "ok"
//        ]);

        $validationRule = [
            'group_main_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[group_main_image]'
                    . '|is_image[group_main_image]'
                    . '|mime_in[group_main_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[group_main_image,1000]'
                    . '|max_dims[group_main_image,1924,1924]',
            ],
        ];
        //요청 데이터 중 group_main_image 요소에 대한 이미지 검증
        if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
            $data = [
                'result' => false,
                'errors' => $this->validator->getErrors()
            ];
            return $res->setJSON($data);
        }

        //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
        $filePath = $imgData->store();
        log_message("debug", "[Group] createGroup \$filePath: ".print_r($filePath, true));

        $data = [
            'user_no' => $req->getVar('user_no'),
            'group_name' => $req->getVar('group_name'),
            'group_desc' => $req->getVar('group_desc'),
            'group_main_image' => $filePath,
            'create_date' => date('Y-m-d H:i:s'),
        ];

        try {
            //모임을 만들고 반환된 모임 번호를 이용해 만든이를 모임장으로서 모임멤버로 추가시켜줌.
            $result = $group->insert($data);
            $result = $groupMember->insert([
                'group_no' => $result,
                'user_no' => $req->getVar('user_no')
            ]);
            log_message("debug", "[Group] createGroup \$result: ". print_r($result, true));

            return $res->setJSON([
                "result" => $result,
                "msg" => "ok"
            ]);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    //모임목록 가져오기
    public function getGroupL() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] createGroup \$data: ". print_r($data, true));

        $sql = "";
        $orderBy = "";
        if ($data['sortState'] == "name") {
            $orderBy = "group_name";
            $sql = " select * 
                    from `Group` g
                    where g.user_no = ? ORDER by 'group_name'  ";

        } else if($data['sortState'] == "member"){
            $sql = " select count(g.user_no) cc, g.* 
                    from `Group` g
                    join GroupMember gm on g.group_no = gm.group_no 
                    where g.user_no = ?
                    group by g.group_no ORDER by cc desc ";

        } else {
            $sql = " select count(gb.gboard_no) cc, g.* 
                    from `Group` g
                    join GroupBoard gb on g.group_no = gb.group_no 
                    where g.user_no = ?
                    group by g.group_no ORDER by cc desc  ";

        }

        try {
//            $result = $group->where("user_no", $data['user_no'])->orderBy($orderBy)->findAll();
            $result = $group->db->query($sql, [$data['user_no']]);
            $result = $result->getResultArray();
            log_message("debug", "[Group] createGroup \$result: ". print_r($result, true));

            return $res->setJSON([
                "result" => $result,
                "msg" => "ok"
            ]);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //모임 상세 가져오기
//    public function getGroupIn() : ResponseInterface
//    {
//
//    }

    //모임의 게시물 목록 가져오기 == 모임상세불러오기()
    public function getGroupIn() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gboardImage = new GroupBoardImage();
        $reply = new Reply();
        $like = new GboardLike();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] getGroupIn \$data: ". print_r($data, true));

        $sql = "";
        $orderBy = "";
        if ($data['sortStateGroupIn'] == "board") {
            $sql = " select * 
                    from GroupBoard gb
                    join User u on gb.user_no = u.user_no
                    where group_no = ? order by gb.create_date desc ";

        } else if($data['sortStateGroupIn'] == "reply"){ //최근 댓글 순 정렬
            $sql = " 나중에 댓글기능 만들고 작성 하자!  ";
        }

        $sql2 = " select * from GroupBoardImage
                    where group_no = ? ";

        try {
            $group->db->transStart();
            //해당 번호의 모임의 정보를 가져온다. 최상위 배열이다.
            $resultFinal = $group->where("group_no", $data['group_no'])->find(); //find메소드는 객체반환하는데 키값이 "0"이다..;; 배열도 아닌것이..

            //각 게시물의 배열을 가져온다.
            $result = $group->db->query($sql, [$data['group_no']]);
            $result = $result->getResultArray();

            //각 게시물에 해당하는 이미지들, 좋아요여부, 수를 가져와서 하위 배열요소로 추가해준다.
            foreach ($result as $i => $board) { //받은 이미지파일배열을 반복문으로 돌려 ci기본 upload경로(writable/uploads)에 저장
                $boardImage = $gboardImage->where('gboard_no', $board['gboard_no'])->findAll();
                $result[$i]['gboard_image'] = $boardImage;

                //해당 유저가 게시물에 좋아요를 눌렀는지 확인 후 없으면 false 추가, 있으면 true 추가
                if ($like->where('user_no', $data['user_no'])->
                            where('gboard_no', $board['gboard_no'])->find() != null) {
                    $result[$i]['is_like'] = true;
                } else {
                    $result[$i]['is_like'] = false;
                }
                //해당 게시물 좋아요 수
                $result[$i]['gboard_like_count'] = $like->where('gboard_no', $board['gboard_no'])->countAllResults();
                //해당 게시물 댓글 최신 2개
                $result[$i]['replyL'] = $reply->join('User', 'User.user_no = Reply.user_no')
                    ->where('gboard_no', $board['gboard_no'])
                    ->orderBy('reply_writedate', 'desc')->findAll(2);
            }
            log_message("debug", "[Group] getGroupIn \$result: ". print_r($result, true));

            //해당 모임의 멤버들 정보를 가져온다. 모임테이블과 조인할일이 있을때는 모임장 번호와 참가멤버 번호의 컬럼명이 안겹치게 alias 를 걸어줌에 주의!!
            $members = $groupMember->join('User', 'User.user_no = GroupMember.user_no')
                ->where('group_no', $data['group_no'])->findAll();

            //최종적으로 최상위 모임 배열에 각 정보들을 하위 요소로써 추가해 넣는다.
            $resultFinal['gboardL'] = $result;
            $resultFinal['memberL'] = $members;

            if ($group->db->transComplete()){
                $group->db->transCommit();
                return $res->setJSON([
                    "result" => $resultFinal,
                    "msg" => "ok"
                ]);

            } else {
                $group->db->transRollback();
                return $res->setJSON([
                    "result" => $group->db->error(),
                    "msg" => "fail"
                ]);
            }

            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.
        } catch (\ReflectionException | DataException |\Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 글쓰기 완료 버튼 클릭시
    public function writeGroupIn() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] writeGroupIn \$data: ". print_r($data, true));
        $imgData3 = $req->getFileMultiple('gboard_image');
        log_message("debug", "[Group] writeGroupIn \$getFileMultiple: ".print_r($imgData3, true));
//        $imgData = $req->getFile('gboard_image');
        $imgData = $req->getFiles();
        log_message("debug", "[Group] writeGroupIn \$getFiles: ".print_r($imgData, true));


        $validationRule = [
            'gboard_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[gboard_image]'
                    . '|is_image[gboard_image]'
                    . '|mime_in[gboard_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[gboard_image,1000]'
                    . '|max_dims[gboard_image,1924,1924]',
            ],
        ];
        //request 된 데이터 중 gboard_image 요소에 대한 이미지 검증 - 파일 오브젝트의 규칙을 검사
        if (sizeof($imgData)!=0) {
            if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
                $data = [
                    'result' => false,
                    'errors' => $this->validator->getErrors()
                ];
                return $res->setJSON($data);
            }
        }

        $data = [
            'user_no' => $req->getVar('user_no'),
            'group_no' => $req->getVar('group_no'),
            'gboard_content' => $req->getVar('gboard_content'),
            'create_date' => date('Y-m-d H:i:s'),
        ];

        try {
            //모임을 만들고 반환된 모임 번호를 이용해 만든이를 모임장으로서 모임멤버로 추가시켜줌.
            $result = $groupBoard->insert($data);
            log_message("debug", "[Group] writeGroupIn \$result: ". print_r($result, true));

            //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
            if ($imgData != null) {
                foreach ($imgData['gboard_image'] as $img) { //받은 이미지파일배열을 반복문으로 돌려 ci기본 upload경로(writable/uploads)에 저장
                    if (! $img->hasMoved()) {
                        $originalName = $img->getClientName(); //원래 파일의 이름
                        $filePath =  $img->store(); //store는 기본경로에 날짜폴더를 생성하고 거기에 랜덤이름의 파일을 저장한다. 그 후 저장된 경로를 리턴
                        log_message("debug", "writeGroupIn:\$originalName: ".print_r($originalName, true));
                        log_message("debug", "writeGroupIn:\$filePath: ".print_r($filePath, true));
                        //                $fileName =  $img->getRandomName();
                        //                $img->move(WRITEPATH.'uploads', $fileName); //경로명, 저장될이름명(이이름으로저장됨) -- move는 tmp폴더에 임시로 받아진 사진파일을 지정한 경로에 파일명으로 저장한다. 그후 tmp폴더의 사진파일은 메소드종료와 동시에 삭제됨

                        //서버에 저장된 파일경로를 db에 저장함.
                        $gBoardImage->insert([
                            'gboard_no' => $result,
                            'original_file_name' => $originalName,
                            'stored_file_name' => $filePath,
                            'file_size' => $img->getSize(),
                            'create_date' => date('Y-m-d H:i:s')
                        ]);
                    }
                }
            }

            return $res->setJSON([
                "result" => $result,
                "msg" => "ok"
            ]);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 글수정 버튼 클릭시 - group_no, user_no, gboard_content, gboard_image[]
    public function updateBoardGroupIn() : ResponseInterface
    {
        helper('filesystem');
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] updateBoardGroupIn \$data: ". print_r($data, true));
        $imgData3 = $req->getFileMultiple('gboard_image');
        log_message("debug", "[Group] updateBoardGroupIn \$getFileMultiple: ".print_r($imgData3, true));
//        $imgData = $req->getFile('gboard_image');
        $imgData = $req->getFiles();
        log_message("debug", "[Group] updateBoardGroupIn \$getFiles: ".print_r($imgData, true));


        $validationRule = [
            'gboard_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[gboard_image]'
                    . '|is_image[gboard_image]'
                    . '|mime_in[gboard_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[gboard_image,1000]'
                    . '|max_dims[gboard_image,1924,1924]',
            ],
        ];
        //request 된 데이터 중 gboard_image 요소에 대한 이미지 검증 - 파일 오브젝트의 규칙을 검사
        if (sizeof($imgData)!=0) {
            if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
                $data = [
                    'result' => false,
                    'errors' => $this->validator->getErrors()
                ];
                return $res->setJSON($data);
            }
        }

        $data = [
            'user_no' => $req->getVar('user_no'),
            'group_no' => $req->getVar('group_no'),
            'gboard_content' => $req->getVar('gboard_content'),
        ];

        try {
            $groupBoard->db->transStart();
            //보드넘버를 이용해 업데이트 된 내용 db에 저장
            $result = $groupBoard->update($req->getVar('gboard_no'), $data);
            log_message("debug", "[Group] updateBoardGroupIn \$result: ". print_r($result, true));

            //모임글이미지테이블에서 보드넘버를 이용해 삭제
            if ($imgData != null && count($imgData) > 0 ) {
                //기존의 사진 정보 삭제
                $imgPre = $gBoardImage->where('gboard_no', $req->getVar('gboard_no'))->findColumn('stored_file_name');
                if ($imgPre != null) {
                    foreach ($imgPre as $item) {
                        delete_files('../writable/uploads/' . $item); //경로의 파일 삭제
                    }
                    $gBoardImage->where('gboard_no', $req->getVar('gboard_no'))->delete();//db에서 정보삭제
                }

                //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
                foreach ($imgData['gboard_image'] as $img) { //받은 이미지파일배열을 반복문으로 돌려 ci기본 upload경로(writable/uploads)에 저장
                    if (! $img->hasMoved()) {
                        $originalName = $img->getClientName(); //원래 파일의 이름
                        $filePath =  $img->store(); //store는 기본경로에 날짜폴더를 생성하고 거기에 랜덤이름의 파일을 저장한다. 그 후 저장된 경로를 리턴
                        log_message("debug", "updateBoardGroupIn:\$originalName: ".print_r($originalName, true));
                        log_message("debug", "updateBoardGroupIn:\$filePath: ".print_r($filePath, true));
                        //                $fileName =  $img->getRandomName();
                        //                $img->move(WRITEPATH.'uploads', $fileName); //경로명, 저장될이름명(이이름으로저장됨) -- move는 tmp폴더에 임시로 받아진 사진파일을 지정한 경로에 파일명으로 저장한다. 그후 tmp폴더의 사진파일은 메소드종료와 동시에 삭제됨

                        //서버에 저장된 파일경로를 db에 저장함.
                        $gBoardImage->insert([
                            'gboard_no' => $req->getVar('gboard_no'),
                            'original_file_name' => $originalName,
                            'stored_file_name' => $filePath,
                            'file_size' => $img->getSize(),
                            'create_date' => date('Y-m-d H:i:s')
                        ]);
                    }
                }

            } else {
                //이미지 데이터가 없이 요청왔을 때는 서버에 있을 기존의 사진 정보 삭제 - 클라에서 사진을 다 지우라는 요청이기때문
                $imgPre = $gBoardImage->where('gboard_no', $req->getVar('gboard_no'))->findColumn('stored_file_name');
                if ($imgPre != null) {
                    foreach ($imgPre as $item) {
                        delete_files('../writable/uploads/' . $item); //경로의 파일 삭제
                    }
                    $gBoardImage->where('gboard_no', $req->getVar('gboard_no'))->delete();//db에서 정보삭제
                }
            }

            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);

            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 모임 글삭제 버튼 클릭시 - gboard_no, user_no,
    public function deleteBoardGroupIn() : ResponseInterface
    {
        helper('filesystem');
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] deleteBoardGroupIn \$data: ". print_r($data, true));
//        $imgData = $req->getFile('gboard_image');

        try {
            $groupBoard->db->transStart();
            //이미지 데이터가 없이 요청왔을 때는 서버에 있을 기존의 사진 정보 삭제 - 클라에서 사진을 다 지우라는 요청이기때문
            $imgPre = $gBoardImage->where('gboard_no', $data['gboard_no'])->findColumn('stored_file_name');
            if ($imgPre != null) {
                foreach ($imgPre as $item) {
                    delete_files('../writable/uploads/' . $item); //경로의 파일 삭제
                }
                $gBoardImage->where('gboard_no', $data['gboard_no'])->delete();//db에서 정보삭제
            }

            $result = $groupBoard->delete($data['gboard_no']);
            log_message("debug", "[Group] deleteBoardGroupIn \$result: ". print_r($result, true));

            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 모임글상세가져오기()  - gboard_no, whereIs
    public function getGboardDetail() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gboardImage = new GroupBoardImage();
        $reply = new Reply();
        $like = new GboardLike();
        $req = $this->request;
        $res = $this->response;
        //        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] getGroupIn \$data: " . print_r($data, true));

//        $sql = "";
//        $orderBy = "";
//        if ($data['sortStateGroupIn'] == "board") {
//            $sql = " select *
//                from GroupBoard gb
//                join User u on gb.user_no = u.user_no
//                where group_no = ? order by gb.create_date desc ";
//
//        } else if ($data['sortStateGroupIn'] == "reply") {
//            $sql = " 나중에 댓글기능 만들고 작성 하자!  ";
//        }
//
//        $sql2 = " select * from GroupBoardImage
//                where group_no = ? ";

        try {
            $group->db->transStart();
            //GroupInRva : 게시물 클릭으로 요청한 것이면 글본수 +1해줌
            if ($data['whereIs'] == 'GroupInRva') {
                //mysql은 자기 자신의 테이블을 바로 서브쿼리한 쿼리를 쓰지 못하기에 인라인 뷰로 다시 한번 더 감싸야 가능
                $sql = " update GroupBoard gb set gboard_hit = (
                            select gb2.gboard_hit
                            from (
                            select *
                            from GroupBoard
                            where gboard_no = ? ) gb2 
                        )+1 
                        where gboard_no = ? ";
                $groupBoard->db->query($sql, [$data['gboard_no'], $data['gboard_no']]);

            }
            
            //해당 번호의 모임의 정보를 가져온다. 최상위 배열이다.
            //         $resultFinal = $group->where("group_no", $data['group_no'])->find(); //find메소드는 객체반환하는데 키값이 "0"이다..;; 배열도 아닌것이..

            //각 게시물의 배열을 가져온다.
            //         $result = $group->db->query($sql, [$data['group_no']]);
            //         $result = $result->getResultArray();
            $result = $groupBoard->join('User', 'User.user_no = GroupBoard.user_no')
                ->where('gboard_no', $data['gboard_no'])->find()[0];

            //게시물에 속한 이미지들을 가져와 추가해줌
            $boardImage = $gboardImage->where('gboard_no', $data['gboard_no'])->findAll();
            $result['gboard_image'] = $boardImage;

            //댓글목록 추가하기
            $sqlReply = " select r.*, u.*, u2.user_nick parent_nick
                            from Reply r 
                            join `User` u on r.user_no = u.user_no 
                            LEFT OUTER join `User` u2 on r.parent_reply_writer_no = u2.user_no 
                            where gboard_no = ?
                            order by reply_group , reply_writedate 
                             ";
            $replyRes = $reply->db->query($sqlReply, [$data['gboard_no']])->getResultArray();
//            $replyRes = $reply->join('User', 'User.user_no = Reply.user_no')
//                ->join('User', 'User.user_no = Reply.parent_reply_writer_no')
//                ->where('gboard_no', $data['gboard_no'])
//                //                ->where('parent_reply_no', null)
//                ->orderBy('reply_group', 'ASC')
//                ->orderBy('reply_writedate', 'ASC')->findAll();

            //좋아요 수 추가
            $likeCount = $like->where('gboard_no', $data['gboard_no'])->countAllResults();
            $result['like_count'] = $likeCount;
            //좋아요 여부 추가
            $isLike = $like->where('user_no', $data['user_no'])->
                where('gboard_no', $data['gboard_no'])->countAllResults();
            if ($isLike > 0) {
                $result['is_like'] = true;
            } else {
                $result['is_like'] = false;
            }

            $final = [
                'gboardInfo' => $result,
                'gboardReplyL' => $replyRes
            ];

            if ($group->db->transComplete()) {
                $group->db->transCommit();
                return $res->setJSON([
                    "result" => $final,
                    "msg" => "ok"
                ]);

            } else {
                $group->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }

            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.
        } catch (\ReflectionException | DataException | \Exception $e) {
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 댓글 클릭시 - gboard_no, user_no,
    public function writeGboardReply() : ResponseInterface
    {
        helper('filesystem');
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $reply = new Reply();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] writeGboardReply \$data: ". print_r($data, true));
//        $imgData = $req->getFile('gboard_image');

        try {
            $groupBoard->db->transStart();
//            $result = $reply->insert([
//                'user_no' => $data['user_no'],
//                'gboard_no' => $data['gboard_no'],
//                'reply_content' => $data['reply_content'],
//                'reply_writedate' => date('Y-m-d H:i:s'),
//            ]);
//            log_message("debug", "[Group] writeGboardReply \$result: ". print_r($result, true));
            $result = null;

            if ($data['to'] == 'reply') {
                $result = $reply->insert([
                    'user_no' => $data['user_no'],
                    'gboard_no' => $data['gboard_no'],
                    'reply_content' => $data['reply_content'],
                    'reply_writedate' => date('Y-m-d H:i:s'),
                ]);
                log_message("debug", "[Group] writeGboardReply \$result: ". print_r($result, true));
                $reply->set('reply_group', $result)->update($result); // 자신의 키를 댓글그룹 최상위로 등록

            } else if ($data['to'] == 'answer') {
                $result = $reply->insert([
                    'user_no' => $data['user_no'],
                    'gboard_no' => $data['gboard_no'],
                    'reply_content' => $data['reply_content'],
                    'parent_reply_no' => $data['parent_reply_no'],
                    'parent_reply_writer_no' => $data['parent_reply_writer_no'],
                    'reply_group' => $data['reply_group'],
                    'reply_writedate' => date('Y-m-d H:i:s'),
                ]);
                log_message("debug", "[Group] writeGboardReply \$result: ". print_r($result, true));
            }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 모임 댓글 삭제 클릭시 - gboard_no, user_no,
    public function deleteGboardReply() : ResponseInterface
    {
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $reply = new Reply();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] deleteGboardReply \$data: ". print_r($data, true));
//        $imgData = $req->getFile('gboard_image');

        try {
            $groupBoard->db->transStart();
            $result = $reply->delete( $data['reply_no']);
            log_message("debug", "[Group] deleteGboardReply \$result: ". print_r($result, true));

            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 댓글 수정 클릭시 - gboard_no, user_no,
    public function modifyGboardReply() : ResponseInterface
    {
        helper('filesystem');
        $group = new \App\Models\Group();
        $groupMember = new GroupMember();
        $groupBoard = new GroupBoard();
        $gBoardImage = new GroupBoardImage();
        $reply = new Reply();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] modifyGboardReply \$data: ". print_r($data, true));
//        $imgData = $req->getFile('gboard_image');

        try {
            $groupBoard->db->transStart();
            $result = $reply->update($data['reply_no'], [
                'reply_content' => $data['reply_content'],
            ]);
            log_message("debug", "[Group] modifyGboardReply \$result: ". print_r($result, true));
//            $reply->set('reply_group', $result)->update($result); // 자신의 키를 댓글그룹 최상위로 등록


            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 모임 좋아요 클릭시 - gboard_no, user_no,
    public function clickGboardLike() : ResponseInterface
    {
        $groupBoard = new GroupBoard();
        $like = new GboardLike();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] modifyGboardLike \$data: ". print_r($data, true));
//        $imgData = $req->getFile('gboard_image');

        try {
            $groupBoard->db->transStart();
            //해당 게시물과 유저번호에 부합하는 정보가 존재하지 않는다면(0) insert, 존재한다면 delete
            $count = $like->where('gboard_no', $data['gboard_no'])->where('user_no', $data['user_no'])->countAllResults();
            if ($count == 0) {
                $like->insert([
                    'user_no' => $data['user_no'],
                    'gboard_no' => $data['gboard_no'],
                ]);

            } else {
                $like->where('gboard_no', $data['gboard_no'])->where('user_no', $data['user_no'])
                ->delete();
            }
//            log_message("debug", "[Group] modifyGboardLike \$result: ". print_r($result, true));

            if ($groupBoard->db->transComplete()){
                $groupBoard->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $groupBoard->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }




}