<?php

namespace App\Controllers;

use App\Models\Challenge;
use App\Models\ChallengeDetail;
use App\Models\ChallengeDetailLike;
use App\Models\ChallengeDetailVerse;
use App\Models\ChallengeSelectedBible;
use App\Models\ChallengeVideo;
use App\Models\Chat;
use App\Models\ChatImage;
use App\Models\ChatRoom;
use App\Models\ChatRoomInfo;
use App\Models\GboardLike;
use App\Models\GroupBoard;
use App\Models\GroupBoardImage;
use App\Models\GroupMember;
use App\Models\GroupMemberInvite;
use App\Models\Note;
use App\Models\NoteVerse;
use App\Models\Reply;
use App\Models\User;
use CodeIgniter\Database\Exceptions\DataException;
use CodeIgniter\HTTP\ResponseInterface;
use CodeIgniter\Log\Logger;
use Config\Pager;
use Streaming\FFMpeg;
use Streaming\Representation;

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
                'user_no' => $req->getVar('user_no'),
                'user_grade' => '모임장'
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


    // 모임 이미지 선택 버튼 클릭시 (GroupInFm - image)
    public function groupProfileImageSelect() : ResponseInterface
    {
        helper('filesystem');
        $user = new User();
        $group = new \App\Models\Group();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] groupProfileImageSelect \$data: ". print_r($data, true));
//        $imgData3 = $req->getFileMultiple('product_image');
//        log_message("debug", "[Group] createGroup \$getFileMultiple: ".print_r($imgData3, true));
        $imgData = $req->getFile('group_main_image');
//        $imgData = $req->getFiles();
        log_message("debug", "[Group] groupProfileImageSelect \$getFiles: ".print_r($imgData, true));

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

        try {
            //요청 데이터 중 user_image 요소에 대한 이미지 검증
            if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
                $data = [
                    'result' => false,
                    'errors' => $this->validator->getErrors()
                ];
                return $res->setJSON($data);
            }

            //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
            $filePath = $imgData->store();
            log_message("debug", "[Group] groupProfileImageSelect \$filePath: ".print_r($filePath, true));

            //기존의 사진 파일 서버저장소에서 삭제
            $imgPre = $group->where('group_no', $req->getVar('group_no'))->findColumn('group_main_image');
            log_message("debug", "[Group] groupProfileImageSelect \$imgPre delete: ".print_r($imgPre, true));
            if ($imgPre != null && count($imgPre) != 0) {
                if ($imgPre[0] != null && $imgPre[0] != "") {  // 연관배열 [0] =>     <<이것때문에 몇번이나 파일 다날라감..주의!!
                    delete_files('../writable/uploads/' . $imgPre[0]); //경로의 파일 삭제
                }
            }

            //위에서 얻은 이미지 경로를 db에 업데이트시켜줌
            $result = $group->set('group_main_image', $filePath)->update($req->getVar('group_no'));
            log_message("debug", "[Group] groupProfileImageSelect \$result: ". print_r($result, true));

            $result = $group->where('group_no', $req->getVar('group_no'))->findColumn('group_main_image');

            return $res->setJSON([
                "result" => $result[0],
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
        log_message("debug", "[Group] getGroupL \$data: ". print_r($data, true));

        $namedBinding = [
            'user_no' => $data['user_no']
        ];

        $sql = "";
        $orderBy = "";
        if ($data['sortState'] == "name") {
            $orderBy = "group_name";
//            $sql = " select *
//                    from `Group` g
//                    where g.user_no = ? ORDER by 'group_name'  ";
            $sql = " select * 
                    from `Group` g
                    join GroupMember gm on g.group_no = gm.group_no
                    where g.group_no in (select group_no 
                                            from `GroupMember` g
                                            where g.user_no = :user_no: ORDER by group_name)
                                    and gm.user_no = :user_no:
                    ORDER by group_name ";

        } else if($data['sortState'] == "member"){
            $sql = " select count(g.user_no) cc, g.* 
                    from `Group` g
                    join GroupMember gm on g.group_no = gm.group_no 
                    where g.user_no = :user_no:
                    group by g.group_no ORDER by cc desc ";

        } else {
            $sql = " select count(gb.gboard_no) cc, g.* 
                    from `Group` g
                    join GroupBoard gb on g.group_no = gb.group_no 
                    where g.user_no = :user_no:
                    group by g.group_no ORDER by cc desc  ";

        }

        try {
//            $result = $group->where("user_no", $data['user_no'])->orderBy($orderBy)->findAll();
            $result = $group->db->query($sql, $namedBinding/*[$data['user_no']]*/);
            $result = $result->getResultArray();
            log_message("debug", "[Group] getGroupL \$result: ". print_r($result, true));

            return $res->setJSON([
                "result" => $result,
                "msg" => "ok"
            ]);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



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
                //해당 게시물 총 댓글 수
                $result[$i]['reply_count'] = $reply->where('gboard_no', $board['gboard_no'])->countAllResults();
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



    // 챌린지 만들기 총 절수 가져오기
    public function getCountVerseForChalCreate() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] getCountVerseForChalCreate \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            $totalCount = 0;
            foreach ($data as $i => $item) {
                $res1 = $bible->join('book', 'book.book_no = bible_korHRV.book')
                    ->where('book', $item['book'])->countAllResults();
                $totalCount += $res1;
            }
            log_message("debug", "[Group] getCountVerseForChalCreate \$totalCount: ". print_r($totalCount, true));

            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => $totalCount,
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 챌린지 만들기 - 계산방식:계산된결과, 선택된 성경목록, 사용자번호, 모임번호
    public function createChallenge() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $chal = new Challenge();
        $cSelected = new ChallengeSelectedBible();
        $cDetail = new ChallengeDetail();
        $cDetailVerse = new ChallengeDetailVerse();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] createChallenge \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            $res1 = $chal->insert([
               'user_no' => $data['user_no'],
               'group_no' => $data['group_no'],
               'chal_title' => $data['chal_title'],
               'chal_create_date' => date('Y-m-d H:i:s')
            ]);

            //챌린지 선택책 테이블에도 책정보 insert
            $insertData = [];
            $insertDetailItems = [];
            $addedTotalBibleL = [];
            foreach ($data['selectedCreateList'] as $i => $item) { //selectedCreateList 선택된 책 정보 리스트
                $item['chal_no'] = $res1;   //챌린지 insert 결과로 얻은 pk번호를 해당 테이블에 들어갈 데이터의 pk번호로써 추가해줌
                array_push($insertData, $item);
                /////////////////////////////////////////////////////////////////
                //item에 해당하는 책의 구절들을 배열로 가져오고, 그안의 각 레코드(bible_no)들을 챌린지 상세 테이블에 insertBatch
                // 하기 위한 '임시 총 구절 모음 배열'($addedTotalBibleL)을 마련해둠
                foreach ($bible->where('book', $item['book'])->findAll() as $i2 => $item2) {
                    array_push( $addedTotalBibleL, $item2);
                }
            }
            $cSelected->insertBatch($insertData);



            //챌린지 상세정보도 입력
            //전체구절리스트 - 일수로 계산시
            $진행완료일차 = $data['computedDay'];
            $하루필요절개수고정 = floor(count($addedTotalBibleL) / $data['computedDay']); //평균 하루 필요절수
            $하루필요절개수 = 0; //하루 필요 절수에 충족하기 위한 값
            if ($data['whatIsSelected'] == 'day') {
                $진행일 = 1;
                //전체 절을 전체 일수로 나누면 절개수가 산출됨
                //각 진행일을 progress_day 컬럼에 저장해줌
                //하루에 필요절개수를 insert 함 - 필요절개수고정값이 되기전까지 +1
                // 후에 필요절개수 초기화, 진행일 +1 해줌
                // 진행완료일차 값에 도달했을때 특별 코드를 넣음 - 나머지 처리?
                for ($i=1; $i <= $진행완료일차; $i++) {
                    $progressDate수정 = $i-1 ; //진행 첫날부터 진행날짜를 +하면 안됨
                    $insertDetailData = []; //Detail 테이블 정보
                    $insertDetailData['chal_no'] = $res1;
                    $insertDetailData['start_date'] = date('Y-m-d H:i:s');
                    $insertDetailData['progress_date'] = date('Y-m-d H:i:s', strtotime("+{$progressDate수정} days")); //$data['computedDay'] 필요일수
                    $insertDetailData['progress_day'] = $i; //진행일
                    $cDetail->insert($insertDetailData);

                }
                //챌린지 일차 테이블과   각 일차의 절데이터 테이블을 따로 만든다.
                foreach ($addedTotalBibleL as $i => $item){
                    $하루필요절개수++;
                    $날짜계산 = $진행일-1;
                    $insertDetailVerseData = []; //DetailVerse 테이블 정보
                    $insertDetailVerseData['chal_no'] = $res1;
                    $insertDetailVerseData['bible_no'] = $item['bible_no']; //책에 속한 구절들 각각
                    $insertDetailVerseData['progress_day'] = $진행일; //
                    $cDetailVerse->insert($insertDetailVerseData);
                    if ($하루필요절개수 == $하루필요절개수고정) { //필요절개수가 충족되면
                        $하루필요절개수 = 0;                   //초기화후
                        $진행일++;                             //진행일을 +해서 다음 insert 값을 변화시킨다
                    }
                }

                // todo verse 개수로 진행하는 로직은 verse 개수를 받아와서 그걸로 산출해야함
            } /*else {
                $진행일 = 1;
                //전체 절을 전체 일수로 나누면 절개수가 산출됨
                //각 진행일을 progress_day 컬럼에 저장해줌
                //하루에 필요절개수를 insert 함 - 필요절개수고정값이 되기전까지 +1
                // 후에 필요절개수 초기화, 진행일 +1 해줌
                // 진행완료일차 값에 도달했을때 특별 코드를 넣음 - 나머지 처리?
                foreach ($addedTotalBibleL as $i => $item) {
                    $하루필요절개수++;
                    $insertDetailData = [];
                    $insertDetailData['chal_no'] = $res1;
                    $insertDetailData['bible_no'] = $item['bible_no']; //책에 속한 구절들 각각
                    $insertDetailData['start_date'] = date('Y-m-d H:i:s');
                    $insertDetailData['progress_date'] = date('Y-m-d H:i:s', strtotime("+${진행일} days")); //$data['computedDay'] 필요일수
                    $insertDetailData['progress_day'] = $진행일;
                    $cDetail->insert($insertDetailData);
//                    if ($진행완료일차 == $진행일) {
//                    } else {
//                    }
                    if ($하루필요절개수 == $하루필요절개수고정) { //필요절개수가 충족되면
                        $하루필요절개수 = 0;                   //초기화후
                        $진행일++;                             //진행일을 +해서 다음 insert 값을 변화시킨다
                    }
                }
            }*/





            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 챌린지 목록가져오기
    public function getChallengeList() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $chal = new Challenge();
        $cSelected = new ChallengeSelectedBible();
        $cDetail = new ChallengeDetail();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] getChallengeList \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            $result = [];
            $res1 = $chal->join('User', 'User.user_no = Challenge.user_no')
                ->where('Challenge.user_no', $data['user_no'])->where('Challenge.group_no', $data['group_no'])
                ->findAll();

            foreach ($res1 as $i => $item){
                $res1[$i]['selected_bibleL'] = $cSelected
                    ->join('book', 'book.book_no = ChallengeSelectedBible.book')
                    ->where('chal_no', $item['chal_no'])->findAll();
            }

//            $result['chalL'] = $res1;

            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => $res1,
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


     // 챌린지상세목록가져오기
    public function getChallengeDetailList() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $chal = new Challenge();
        $cSelected = new ChallengeSelectedBible();
        $cDetail = new ChallengeDetail();
        $cDetailVerse = new ChallengeDetailVerse();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] getChallengeDetailL \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            // todo 들어가야할 데이터: 진행률 - 해당일차 디테일의 절들 체크수에 따른 진행률 표시
            // todo 책장절 이름 시작 ~ 끝 범위

//            $result = $cDetail->join('bible_korHRV', 'bible_korHRV.bible_no = ChallengeDetail.bible_no')
//                ->join('book', 'book.book_no = bible_korHRV.book')
//                ->where('chal_no', $data['chal_no'])->findAll();
            $sql = "select chal_no, progress_day, is_checked, start_date, progress_date, chal_detail_no 
                    from ChallengeDetail 
                    where chal_no = ? ";
            $result = $cDetail->db->query($sql, [$data['chal_no']])->getResultArray();
//            $result = $cDetail->where('chal_no', $data['chal_no'])->findAll();


//            $res1 = $chal->join('User', 'User.user_no = Challenge.user_no')
//                ->where('Challenge.user_no', $data['user_no'])->where('Challenge.group_no', $data['group_no'])
//                ->findAll();
//
            foreach ($result as $i => $item){
                $minSql = "select * from ChallengeDetailVerse cd 
                        join bible_korHRV bk on bk.bible_no = cd.bible_no
                        join book b on b.book_no = bk.book
                        where cd.bible_no = (select MIN(bible_no)  from ChallengeDetailVerse where progress_day = ? and chal_no = ? )
                        and cd.chal_no = ? ";
                $maxSql = "select * from ChallengeDetailVerse cd 
                        join bible_korHRV bk on bk.bible_no = cd.bible_no
                        join book b on b.book_no = bk.book
                        where cd.bible_no = (select MAX(bible_no)  from ChallengeDetailVerse where progress_day = ? and chal_no = ? )
                         and cd.chal_no = ? ";
                $tmpMin= $cDetail->db->query($minSql, [$i+1, $item['chal_no'], $item['chal_no']])->getResultObject(); //i+1 이유: progress_day 시작이 1부터 시작이기 때문에 싱크맞춰야함
                $tmpMax= $cDetail->db->query($maxSql, [$i+1, $item['chal_no'], $item['chal_no']])->getResultObject();
                //백업
//                $tmpMin= $cDetail->db->query($minSql, [$i+1, $item['chal_no']])->getResultObject(); //i+1 이유: progress_day 시작이 1부터 시작이기 때문에 싱크맞춰야함
//                $tmpMax= $cDetail->db->query($maxSql, [$i+1, $item['chal_no']])->getResultObject();

                $result[$i]['first_verse'] = $tmpMin; //첫번째 절 데이터
                $result[$i]['last_verse'] = $tmpMax; // 두번째 절 데이터
                $result[$i]['verse_count'] = $cDetailVerse->where('progress_day', $item['progress_day'])
                    ->where('chal_no', $item['chal_no'])
                    ->countAllResults(); //해당 일차 인증할 절 개수
                $checkedVerseCount = $cDetailVerse->where('progress_day', $item['progress_day'])
                    ->where('chal_no', $item['chal_no'])->where('is_checked', 1)
                    ->countAllResults(); //체크완료된 절 개수
                //x: 13(checked) = 100: 20(총절수) -> x20 = 1300 -> x = 1300(checked*100) / 20(총절수) f
                //진행율 추가
                $result[$i]['progress_percent'] = round((100*$checkedVerseCount) / $result[$i]['verse_count']);

            }


            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

     // 챌린지상세인증 내용
    public function getChallengeDetailVerseList() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $chal = new Challenge();
        $cSelected = new ChallengeSelectedBible();
        $cDetail = new ChallengeDetail();
        $cDetailVerse = new ChallengeDetailVerse();
        $cVideo = new ChallengeVideo();
        $cLike = new ChallengeDetailLike();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] getChallengeDetailVerseList \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            // todo 나중에 들어가야할 데이터: 체크유무, 절 데이터, 영상 데이터

            //인증화면 인증할 절 데이터 - 성경, 책 테이블 정보 조인하여 가져오기
            $res1 = $cDetailVerse
                ->join('bible_korHRV', 'bible_korHRV.bible_no = ChallengeDetailVerse.bible_no')
                ->join('book', 'book.book_no = bible_korHRV.book')
                ->where('progress_day', $data['progress_day'])
                ->where('chal_no', $data['chal_no'])->findAll();

            //인증 비디오 정보 : 있으면 불러온다
            $res2 = $cVideo->where('chal_detail_no', $data['chal_detail_no'])->findAll();

            //챌린지 상세 페이지의 각 좋아요 아이콘의 개별 숫자, 현재 사용자가 선택한 좋아요 현황을 반환해줌
            $sql = " select like_bt_no, count(*) `count`
                    from ChallengeDetailLike
                    where chal_detail_no = ? 
                    group by like_bt_no ";

            $result = [
                'chalDetailVerseL' => $res1,
                'chalDetailVideoInfo' => $res2,
                'likeInfo' => $cLike->where('chal_detail_no', $data['chal_detail_no'])->findAll(),
                'likeMyInfo' => $cLike->where('user_no', $data['user_no'])
                    ->where('chal_detail_no', $data['chal_detail_no'])->find()
            ];

            //연관배열자체가 jsonObject로 들어간다
            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 챌린지상세인증 체크 내용 업데이트
    public function updateChallengeDetailVerse() : ResponseInterface
    {
        $bible = new \App\Models\Bible();
        $chal = new Challenge();
        $cSelected = new ChallengeSelectedBible();
        $cDetail = new ChallengeDetail();
        $cDetailVerse = new ChallengeDetailVerse();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] updateChallengeDetailVerse \$data: ". print_r($data, true));

        try {
            $bible->db->transStart();
            // todo 나중에 들어가야할 데이터: 체크유무, 절 데이터, 영상 데이터

            //인증화면 인증할 절 체크 현황 업데이트
            $cDetailVerse->set('is_checked', $data['is_checked'])
                ->update($data['chal_detail_verse_no']);

            //인증화면 인증할 절 데이터 - 성경, 책 테이블 정보 조인하여 가져오기
            $result = $cDetailVerse
                ->join('bible_korHRV', 'bible_korHRV.bible_no = ChallengeDetailVerse.bible_no')
                ->join('book', 'book.book_no = bible_korHRV.book')
                ->where('progress_day', $data['progress_day'])
                ->where('chal_no', $data['chal_no'])->findAll();

            if ($bible->db->transComplete()){
                $bible->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $bible->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 챌린지 영상업로드 사전작업 - is_checked 를 2로 바꿔줘서 영상변환중이라는 것을 알리기 위함
    public function createChalDetailVideoFirstWork() : ResponseInterface
    {
        $cDetail = new ChallengeDetail();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] createChalDetailVideoFirstWork \$data: ". print_r($data, true));

        try {
            $cDetail->db->transStart();
            //영상업로드시 해당 일차 챌린지 완료 처리
            //is_checked == 1(true--위의 format callback에서 조정) 이면 영상 업로드 변환까지 완료되었다는 뜻.
            // == 2 면 아직 영상작업은 안됐다는 말
            //클라에서는 대기시키는 메시지 띄워야함.
            $result = $cDetail->set('is_checked', 2)->update($data['chal_detail_no']);
            log_message("debug", "[Group] createChalDetailVideoFirstWork \$result: ". print_r($result, true));

            if ($cDetail->db->transComplete()){
                $cDetail->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $cDetail->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 챌린지 상세화면 좋아요 클릭시
    public function chalLikeClicked() : ResponseInterface
    {
        $cLike = new ChallengeDetailLike();
        $cDetail = new ChallengeDetail();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] chalLikeClicked \$data: ". print_r($data, true));

        //좋아요 작업에 필요한 사용자의 행동 정보 (=클라이언트의 ui 조작 정보)
        $params = [
            'chal_detail_no' => $data['chal_detail_no'],
            'user_no' => $data['user_no'],
            'like_bt_no' => $data['like_bt_no'],
            'is_checked' => $data['is_checked']
        ];

        try {
            $cDetail->db->transStart();
            $result = null;
            //현재 챌린지 상세 페이지의 좋아요 데이터에 사용자의 정보가 없다면(=처음클릭이다) insert  
            if ($cLike->where('user_no', $data['user_no'])->where('chal_detail_no', $data['chal_detail_no'])
                    ->countAllResults() == 0) {
                $result = $cLike->insert($params);

            // 아니면 그 사용자의 정보 업데이트 작업
            } else {
                $result = $cLike->where('user_no', $data['user_no'])
                    ->where('chal_detail_no', $data['chal_detail_no'])
                    ->set($params)->update();
            }
            log_message("debug", "[Group] chalLikeClicked \$result: ". print_r($result, true));

            $sql = " select like_bt_no, count(*) `count`
                    from ChallengeDetailLike
                    where chal_detail_no = ? 
                    group by like_bt_no" ;

            //챌린지 상세 페이지의 각 좋아요 관련정보, 현재 사용자가 선택한 좋아요 현황을 반환해줌
            $result = [
                'likeInfo' => $cLike->where('chal_detail_no', $data['chal_detail_no'])->findAll(),
                'likeMyInfo' => $cLike->where('user_no', $data['user_no'])
                    ->where('chal_detail_no', $data['chal_detail_no'])->find()
            ];

            if ($cDetail->db->transComplete()){
                $cDetail->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $cDetail->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 인증 보내기 (챌린지디테일화면) - 보내온 녹화 비디오를 저장하고 스트리밍용파일로 변환한다.
    public function createChalDetailVideo() : ResponseInterface
    {
        $cVideo = new ChallengeVideo();
        $cDetail = new ChallengeDetail();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] createChalDetailVideo \$data: ". print_r($data, true));
//        $imgData3 = $req->getFileMultiple('product_image');
//        log_message("debug", "[Group] createChalDetailVideo \$getFileMultiple: ".print_r($imgData3, true));
        $videoData = $req->getFile('chal_video');
//        $imgData = $req->getFiles();
        log_message("debug", "[Group] createChalDetailVideo \$getFiles: ".print_r($videoData, true));


        /*$validationRule = [
            'group_main_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[group_main_image]'
                    . '|is_image[group_main_image]'
                    . '|mime_in[group_main_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[group_main_image,1000]'
                    . '|max_dims[group_main_image,1924,1924]',
            ],
        ];
        if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
            $data = [
                'result' => false,
                'errors' => $this->validator->getErrors()
            ];
            return $res->setJSON($data);
        }
        */

        //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
        $filePath = $videoData->store();
        log_message("debug", "[Group] createChalDetailVideo \$filePath: ".print_r(WRITEPATH."uploads/".$filePath, true));

        //받은 비디오파일을 이용해 스트리밍용 파일 생성 - 변환시 사용할 config 파일 설정.
        // ffmpeg 바이너리 지정 및 변환시간 제한, 사용할 쓰레드 수 설정
        $config = [
            'ffmpeg.binaries'  => '/usr/bin/ffmpeg',
            'ffprobe.binaries' => '/usr/bin/ffprobe',
            'timeout'          => 3600, // The timeout for the underlying process
            'ffmpeg.threads'   => 1,   // The number of threads that FFmpeg should use
        ];
        //변환에 사용할 가변 비트레이트를 설정한다. 밑의 addRepresentations 의 인수로 넣는다
        $r_360p  = (new Representation)->setKiloBitrate(276)->setResize(640, 360);
//        $r_480p  = (new Representation)->setKiloBitrate(750)->setResize(854, 480);
//        $r_720p  = (new Representation)->setKiloBitrate(2048)->setResize(1280, 720);
//        $r_1080p = (new Representation)->setKiloBitrate(4096)->setResize(1920, 1080);

        //비디오 변환에 관한 로그출력 지정자 설정
        $log = new Logger(new \Config\Logger());
//        $log->pushHandler(new StreamHandler('/var/log/ffmpeg-streaming.log')); // path to log file

        //확장자 제거하고 변환될 파일명으로 사용한다
        $without_extension = substr($filePath,0,strrpos($filePath,"."));
        log_message("debug", "[Group] createChalDetailVideo \$without_extension: ".print_r($without_extension, true));

        //php_ffmpeg 라이브러리를 이용해 스트리밍용 dash파일을 생성하기 위한 사전작업.
        $ffmpeg = FFMpeg::create($config, $log);
        $video = $ffmpeg->open(WRITEPATH."uploads/".$filePath);
//        $format = new \FFMpeg\Format\Video\X264();
        $format = new \Streaming\Format\X264();


        try {
            $cVideo->db->transStart();

            //영상업로드시 해당 일차 챌린지 완료 처리
            //is_checked == 1(true--위의 format callback에서 조정) 이면 영상 업로드 변환까지 완료되었다는 뜻.
            // == 2 면 아직 영상작업은 안됐다는 말
            //클라에서는 대기시키는 메시지 띄워야함.
            $result = $cDetail->set('is_checked', 2)->update($data['chal_detail_no']);
//            $cVideo->db->transCommit();
            log_message("debug", "[Group] createChalDetailVideo \$result: ". print_r($result, true));

            $res1 = $cVideo->insert([
                'chal_detail_no' => $data['chal_detail_no'],
                'chal_no' => $data['chal_no'],
                'progress_day' => $data['progress_day'],
                'origin_file_name' => $videoData->getClientName(),
                'stored_file_name' => $filePath,
                'file_size' => $videoData->getSize(),
                'video_create_date' => date('Y-m-d H:i:s'),
                'for_streaming_file_name' => $without_extension.".mpd"
            ]);

            $data2 = $data['chal_detail_no'];
            //비디오 변환 작업 후 콜백받아서 변환완료 되었다는 update 작업해줌. true == 1
            $format->on('progress', function ($video, $format, $percentage) use ( $data2) {
                 log_message("debug", "[Group] createChalDetailVideo \$result test in: ". print_r($percentage, true));
                if ($percentage >= 96) {
                    log_message("debug", "[Group] createChalDetailVideo \$result test out: ". print_r($data2, true));
                    $cDetail = new ChallengeDetail();
                    $cDetail->set('is_checked', true)->update($data2);
//                    $cDetail->db->transCommit();
                }
            });

            $video->dash()
//            ->setSegDuration(30) // Default value is 10. 세그먼트 초단위 설정
                ->setAdaption('id=0,streams=v id=1,streams=a') // Set the adaption.
//            ->x264() // Format of the video. Alternatives: x264() and vp9()
                ->setFormat($format)
//            ->autoGenerateRepresentations() //여러가지 가변비트레이트 자동생성 - 느리다 하지말자!
                ->addRepresentations([ $r_360p]) //사용가능한 적응형 가변비트레이트의 종류를 지정
                ->save(WRITEPATH."uploads/".$without_extension); //변환될 파일의 저장위치를 설정



//            if (! $img->hasMoved()) {
//                $originalName = $img->getClientName(); //원래 파일의 이름
//                $filePath =  $img->store(); //store는 기본경로에 날짜폴더를 생성하고 거기에 랜덤이름의 파일을 저장한다. 그 후 저장된 경로를 리턴
//                log_message("debug", "writeGroupIn:\$originalName: ".print_r($originalName, true));
//                log_message("debug", "writeGroupIn:\$filePath: ".print_r($filePath, true));
//                //                $fileName =  $img->getRandomName();
//                //                $img->move(WRITEPATH.'uploads', $fileName); //경로명, 저장될이름명(이이름으로저장됨) -- move는 tmp폴더에 임시로 받아진 사진파일을 지정한 경로에 파일명으로 저장한다. 그후 tmp폴더의 사진파일은 메소드종료와 동시에 삭제됨
//
//                //서버에 저장된 파일경로를 db에 저장함.
//                $gBoardImage->insert([
//                    'gboard_no' => $result,
//                    'original_file_name' => $originalName,
//                    'stored_file_name' => $filePath,
//                    'file_size' => $img->getSize(),
//                    'create_date' => date('Y-m-d H:i:s')
//                ]);
            if ($cVideo->db->transComplete()){
                $cVideo->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $cVideo->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 모임 멤버 초대 링크 생성 클릭시
    public function memberInviteLink() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteLink \$data: ". print_r($data, true));

        try {
            //링크 코드 생성 - 생성하고 db저장하고 클라이언트에 코드를 보내준다.
            $code = sprintf("%06d", mt_rand(1, 999999));
            $invite->db->transStart();

            $result = $invite->insert([
                'group_no' => $data['group_no'],
                'invite_code' => $code,
                'expire_date' => date('Y-m-d H:i:s', strtotime("+2 hours")),
                'code_type' => 'link'
            ]);
//            log_message("debug", "[Group] memberInviteLink \$result: ". print_r($result, true));
            $result = $invite->find($result);


            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 멤버 초대 링크 확인하기 - 링크타고 startFm - LoginActivity - MainActivity - GroupFm(onResume)에서 확인
    // group_no , invite_code, user_no
    public function memberInviteLinkConfirm() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $group = new \App\Models\Group();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteLinkConfirm \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();
            $result = null;
            $res1 = $invite->where('group_no', $data['group_no'])
                ->where('invite_code', $data['invite_code'])
                ->find();
            log_message("debug", "[Group] memberInviteLinkConfirm \$res1: ". print_r($res1, true));

            //db에 해당모임의 초대 비밀번호가 존재하고, 현재시간보다 크다면 == 지나지않았다면 유효기간이 남았다면
            //해당 초대 링크 정보를 클라에 보내줌
            if ($res1 != null && $res1[0]['expire_date'] > date('Y-m-d H:i:s')) {
                //where로찾으면 array로 가져옴. find(id)로 찾으면 객체로 가져옴.. 주의!
                //find()를 쓸때는 where을 써서 인덱스를 쓸 수 있게 하자!
                $res2 = $group->where('group_no', $data['group_no'])->find();
                $result = [
                    'invite_info' => $res1[0], // null 이 아니므로 0번에 있겠지?
                    'group_info' => $res2[0]
                ];
            }
            log_message("debug", "[Group] memberInviteLinkConfirm \$result: ". print_r($result, true));

            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 멤버 초대 링크 확인하기 - 링크타고 startFm - LoginActivity - MainActivity - GroupFm(onResume)에서 확인
    // 이후 위의 확인하기 요청에서 다이얼로그에서 참가하기 클릭시 이 요청 실행 - 해당 모임의 멤버로 추가
    // group_no , invite_code, user_no
    public function memberInviteLinkMemberAdd() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $gMember = new GroupMember();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteLinkMemberAdd \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();

            //참가하기 버튼 클릭시 처리되는 insert 작업 - 사용자를 멤버로 추가
            $result = $gMember->where('group_no', $data['group_no'])
                ->insert([
                    'user_no' => $data['user_no'],
                    'group_no' => $data['group_no']
                ]);

            log_message("debug", "[Group] memberInviteLinkMemberAdd \$result: ". print_r($result, true));
            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    //모임 참가 비밀번호로 멤머 추가하기 - invite_code, user_no
    public function memberInviteNumberMemberAdd() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $gMember = new GroupMember();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteNumberMemberAdd \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();
            $result = null;
            //일단 코드부터 확인 - 레코드가 존재하는지 null부터 체크하고, 유효기간 남았는지 확인하기
            $res1 = $invite->where('invite_code', $data['invite_code'])->find();
            log_message("debug", "[Group] memberInviteNumberMemberAdd \$res1: ". print_r($res1, true));

            if ($res1 != null && $res1[0]['expire_date'] > date('Y-m-d H:i:s')) {
                //존재하고, 유효기간도 남았으면 멤버로 추가하기
                $result = $gMember->where('group_no', $res1[0]['group_no']) //존재하는 레코드안의 모임번호를 이용하여 추가
                    ->insert([
                        'user_no' => $data['user_no'],
                        'group_no' => $res1[0]['group_no']
                    ]);
            }
            log_message("debug", "[Group] memberInviteNumberMemberAdd \$result: ". print_r($result, true));

            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => "ok",
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    //모임 참가 비밀번호로 멤머 추가하기 전 코드 유효성 검사 - invite_code, user_no
    public function memberInviteNumberVerify() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $gMember = new GroupMember();
        $group = new \App\Models\Group();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteNumberMemberAdd \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();
            $result = null;
            //일단 코드부터 확인 - 레코드가 존재하는지 null부터 체크하고, 유효기간 남았는지 확인하기
            $res1 = $invite->where('invite_code', $data['invite_code'])->find();
            log_message("debug", "[Group] memberInviteNumberMemberAdd \$res1: ". print_r($res1, true));

            if ($res1 != null && $res1[0]['expire_date'] > date('Y-m-d H:i:s')) {
                //존재하고 유효기간 남았으면 모임의 정보를 보내주고, 클라에서 이 모임에 참가할건지 물어본뒤 참가여부결정하게함
                $result = $group->where('group_no', $res1[0]['group_no'])->find()[0];
            }
            log_message("debug", "[Group] memberInviteNumberMemberAdd \$result: ". print_r($result, true));

            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



    // 모임 멤버 초대 번호 있는지 확인 - group_no , code_type
    public function isInviteNumber() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] isInviteNumber \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();
            $result = null;
            //client에서 받은 정보를 이용해 참가 코드를 확인하고, 유효한 코드인지 날짜를 비교확인한다.
            //유효하면 유효하다는 의미로 그 레코드를 클라이언트에 보내준다.
            //만료됐으면 expired 라는 메시지를 보내준다
            $res1 = $invite->where('group_no', $data['group_no'])
                ->where('code_type', 'number')
                ->find();
            log_message("debug", "[Group] isInviteNumber \$res1: ". print_r($res1, true));
            //쿼리가 널이 아니고, 컬럼의 number type도 존재한다면(=number 값이 존재하는 행이 있다는 말은 해당 모임에
            //초대 공유 비밀번호가 생성되어 있다는 말임!
            if ($res1 != null && $res1[0]['code_type'] != null) {
                if (date("Y-m-d H:i:s") < $res1[0]['expire_date']) {
                    //아직 유효한 기간임
                    $result = [
                        'invite_no' => $res1[0]['invite_no'],
                        'group_no' => $res1[0]['group_no'],
                        'expire_date' => $res1[0]['expire_date'],
                        'invite_code' => $res1[0]['invite_code'],
                        'code_type' => 'number',
                    ];
                } else {
                    $result = "expired";
                }
            } else { //만료되거나, 비밀번호가 생성되지 않았음.
                $result = "nocreated";
            }
            log_message("debug", "[Group] isInviteNumber \$result: ". print_r($result, true));


            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 모임 멤버 초대 번호 생성 클릭시
    public function memberInviteNumber() : ResponseInterface
    {
        $invite = new GroupMemberInvite();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberInviteNumber \$data: ". print_r($data, true));

        try {
            $invite->db->transStart();
            $code = sprintf("%06d", mt_rand(1, 999999));
            $result = null;
            //재생성 요청이면 1, 초기 생성 요청이면 0 << 초기생성이면 백엔드에서 기존 number 레코드 delete할 필요없음
            //재생성 요청이면 기존의 number 값을 가진 레코드를 지우고 새로 insert 해야하기에 delete 작업해줌
            if ($data['is_recreate'] == 1) {
                $invite->where('code_type', 'number')->where('group_no', $data['group_no'])
                    ->delete();
            }
            $res1 = $invite->insert([
                'group_no' => $data['group_no'],
                'invite_code' => $code,
                'expire_date' => date('Y-m-d H:i:s', strtotime("+2 hours")),
                'code_type' => 'number'
            ]);
            
            $result = $invite->find($res1); //방금 새로 생성한 비밀번호를 클라이언트에 반환함
            log_message("debug", "[Group] memberInviteNumber \$result: ". print_r($result, true));

            if ($invite->db->transComplete()){
                $invite->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $invite->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 모임 멤버 목록 로드
    public function memberListLoad() : ResponseInterface
    {
        $gMember = new GroupMember();
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberListLoad \$data: ". print_r($data, true));

        try {
            $user->db->transStart();
            $result = null;
            $res1 = $gMember->join('User', 'User.user_no = GroupMember.user_no')
                ->where('group_no', $data['group_no'])
                ->orderBy('user_nick')
                ->findAll();

            log_message("debug", "[Group] memberListLoad \$result: ". print_r($result, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $res1,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

 // 모임 멤버 추방 - user_no, group_no
    public function memberFire() : ResponseInterface
    {
        $gMember = new GroupMember();
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberFire \$data: ". print_r($data, true));

        try {
            $user->db->transStart();
            $result = null;
            //해당 멤버를 모임에서 삭제(추방)
            $res1 = $gMember->where('group_no', $data['group_no'])
                ->where('user_no', $data['user_no'])
                ->delete();

            //그리고, 멤버목록을 다시 보내준다.
            $result = $gMember->join('User', 'User.user_no = GroupMember.user_no')
                ->where('group_no', $data['group_no'])
                ->orderBy('user_nick')
                ->findAll();
            log_message("debug", "[Group] memberFire \$result: ". print_r($result, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

// 모임 멤버 탈퇴 - user_no, group_no -- 추방이랑 같음..;
    public function memberOut() : ResponseInterface
    {
        $gMember = new GroupMember();
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberOut \$data: ". print_r($data, true));

        try {
            $user->db->transStart();
            $result = null;
            //해당 멤버를 모임에서 삭제(추방)
            $res1 = $gMember->where('group_no', $data['group_no'])
                ->where('user_no', $data['user_no'])
                ->delete();

            //그리고, 멤버목록을 다시 보내준다.
            $result = $gMember->join('User', 'User.user_no = GroupMember.user_no')
                ->where('group_no', $data['group_no'])
                ->orderBy('user_nick')
                ->findAll();
            log_message("debug", "[Group] memberOut \$result: ". print_r($result, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 모임 멤버 검색 - user_no, group_no, search_word
    public function memberListSearch() : ResponseInterface
    {
        $gMember = new GroupMember();
        $user = new User();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
//        $data = $req->getVar();
        log_message("debug", "[Group] memberListSearch \$data: ". print_r($data, true));

        try {
            $user->db->transStart();
            $result = null;

            //그리고, 멤버목록을 다시 보내준다.
            if ($data['search_word'] == "") {
                $result = $gMember->join('User', 'User.user_no = GroupMember.user_no')
                    ->where('group_no', $data['group_no'])
                    ->orderBy('user_nick')
                    ->findAll();

            } else {
                $result = $gMember->join('User', 'User.user_no = GroupMember.user_no')
                    ->where('group_no', $data['group_no'])
                    ->like('user_nick', $data['search_word'])
                    ->orderBy('user_nick')
                    ->findAll();
            }
            log_message("debug", "[Group] memberListSearch \$result: ". print_r($result, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
                return $res->setJSON([
                    "result" => "failed",
                    "msg" => "fail"
                ]);
            }
        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 채팅방 만들기 클릭시 (GroupInChatCreateDialogFm)
    public function chatRoomCreate() : ResponseInterface
    {
        helper('filesystem');
        $user = new User();
        $chatRoom = new ChatRoom();
        $chatRoomInfo = new ChatRoomInfo();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[group] chatRoomCreate \$data: ". print_r($data, true));
//        $imgData3 = $req->getFileMultiple('product_image');
//        log_message("debug", "[Group] createGroup \$getFileMultiple: ".print_r($imgData3, true));
        $imgData = $req->getFile('chat_room_image');
//        $imgData = $req->getFiles();
        log_message("debug", "[group] chatRoomCreate \$getFiles: ".print_r($imgData, true));

        $validationRule = [
            'chat_room_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[chat_room_image]'
                    . '|is_image[chat_room_image]'
                    . '|mime_in[chat_room_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[chat_room_image,1000]'
                    . '|max_dims[chat_room_image,1924,1924]',
            ],
        ];

        try {
            $user->db->transStart();
            //요청 데이터 중 chat_room_image 요소에 대한 이미지 검증
            if (! $this->validate($validationRule)) { //이미지파일 검증실패시 result false로 에러메시지 리턴.
                $data = [
                    'result' => false,
                    'errors' => $this->validator->getErrors()
                ];
                return $res->setJSON($data);
            }

            //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
            $filePath = $imgData->store();
            log_message("debug", "[group] chatRoomCreate \$filePath: ".print_r($filePath, true));

            //채팅방 만들고 그 채팅방 pk를 이용해서 채팅방 참가자리스트 테이블에 owner_no(방장)을 추가
            $result = $chatRoomInfo->insert([
                'chat_room_title' => $data['chat_room_title'],
                'chat_room_desc' => $data['chat_room_desc'],
                'owner_no' => $data['user_no'],
                'create_date' => date('Y-m-d H:i:s'),
                'chat_room_image' => $filePath,
                'group_no' => $data['group_no'],
            ]);
            $chatRoom->insert([
                'chat_room_no' => $result,
                'user_no' => $data['user_no'],
                'user_chat_join_date' => date('Y-m-d H:i:s')
            ]);

            log_message("debug", "[group] chatRoomCreate \$result: ". print_r($result, true));

//            //추가된 방번호를 이용해 그 방의 유저목록을 클라이언트로 내보내준다.
//            $result = $chatRoomInfo->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
//                ->join('User', 'User.user_no = ChatRoom.user_no')
//                ->where('chat_room_no', $result)
//                ->findAll();

            //채팅방 정보를 클라이언트에 보내준다.
            $res1 = $chatRoomInfo->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
                ->join('User', 'User.user_no = ChatRoomInfo.owner_no')
                ->where('ChatRoomInfo.chat_room_no', $result)
                ->where('group_no', $data['group_no'])
                ->find();

            //추가된 정보를 포함한 채팅방 유저리스트 정보를 클라이언트에 보내준다.
            $res2 = $chatRoomInfo->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
                ->join('User', 'User.user_no = ChatRoom.user_no')
                ->where('ChatRoomInfo.chat_room_no', $result) // todo 문제 생길여지 있음 - 생기면 ChatRoom으로 바꿔보기
                ->where('group_no', $data['group_no'])
                ->findAll();

            //각유저가 쓴 채팅 리스트를 클라이언트에 보내준다.
//            $res3 = $chat->join('User', 'User.user_no = Chat.user_no')
//                ->join('ChatImage', 'ChatImage.chat_no = Chat.chat_no')
//                ->where('chat_room_no', $data['chat_room_no'])
//                ->where('group_no', $data['group_no'])
//                ->findAll();
//            log_message("debug", "[group] chatRoomJoin \$res2: ". print_r($res2, true));

            $result = [
                'chat_room_info' => $res1,
                'chat_room_userL' => $res2
//                'chat_list' => $res3
            ];
            log_message("debug", "[group] chatRoomCreate \$result: ". print_r($result, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
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


    // 채팅방 목록 (GroupInChatFm)
    public function chatRoomList() : ResponseInterface
    {
        $user = new User();
        $chatRoom = new ChatRoom();
        $chatRoomInfo = new ChatRoomInfo();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[group] chatRoomCreate \$data: ". print_r($data, true));

        try {
            $user->db->transStart();

            $result = $chatRoomInfo
//                ->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
                ->join('User', 'User.user_no = ChatRoomInfo.owner_no')
                ->where('group_no', $data['group_no'])
                ->findAll();



            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
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


    // 채팅방 클릭시 (GroupInChatRva)
    public function chatRoomJoin() : ResponseInterface
    {
        $user = new User();
        $chatRoom = new ChatRoom();
        $chatRoomInfo = new ChatRoomInfo();
        $chatImage = new ChatImage();
        $chat = new Chat();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[group] chatRoomJoin \$data: ". print_r($data, true));

        try {
            $user->db->transStart();

            //참가한 유저가 이방에 이미 참가해 있다면 유저를 추가하지 않고 채팅방과 채팅내역 정보만 보내준다.
            //참가하지 않았으면 insert 작업 해준다.
            $count = $chatRoom->where('user_no', $data['user_no'])
                ->where('chat_room_no', $data['chat_room_no'])
                ->countAllResults();
            log_message("debug", "[group] chatRoomJoin \$count: ". print_r($count, true));
            if ( $count == 0) {
                //채팅방 입장시 해당 유저를 채팅방 참가자 테이블에 추가시키고
                $chatRoom->where('chat_room_no', $data['chat_room_no'])
                ->insert([
                    'chat_room_no' => $data['chat_room_no'],
                    'user_no' => $data['user_no'],
                    'user_chat_join_date' => date('Y-m-d H:i:s', strtotime("-5 seconds"))
                ]);
                //채팅 소켓서버와 http 웹서버의 시간이 미묘하게 다를수 있다. 그러면 :s 이부분이 web쪽이 조금더 느릴수도있는데
                //이러면 접속알림을 업데이트하는 소켓채팅서버의 date작업시간이 더 빠르게 기록될수도있다.
                //그러면 접속알림을 보여주는 쿼리가 찰나의 시간차이로 포함되지 않을 수 있음.
                //http웹쪽 서버의 db업데이트 기록시간이 좀더 빨라야 나중에 채팅데이터를 정상적으로 가져올 수 있을듯!
                //그래서 strtotime 에 -5초를 함
            }

            //채팅방 정보를 클라이언트에 보내준다.
            $res1 = $chatRoomInfo->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
                ->join('User', 'User.user_no = ChatRoomInfo.owner_no')
                ->where('ChatRoomInfo.chat_room_no', $data['chat_room_no'])
                ->where('ChatRoomInfo.group_no', $data['group_no'])
                ->find();

            log_message("debug", "[group] chatRoomJoin \$res1: ". print_r($res1, true));
            //추가된 정보를 포함한 채팅방 유저리스트 정보를 클라이언트에 보내준다.
            $res2 = $chatRoomInfo->join('ChatRoom', 'ChatRoomInfo.chat_room_no = ChatRoom.chat_room_no')
                ->join('User', 'User.user_no = ChatRoom.user_no')
                ->where('ChatRoomInfo.chat_room_no', $data['chat_room_no'])
                ->where('ChatRoomInfo.group_no', $data['group_no'])
                ->orderBy('ChatRoom.user_chat_join_date' )
                ->findAll();
            log_message("debug", "[group] chatRoomJoin \$res2: ". print_r($res2, true));

            //각유저가 쓴 채팅 리스트를 클라이언트에 보내준다.
            $sql = "select *,
                    (select COUNT(*) from  ChatIsRead cir where cir.chat_no = c.chat_no ) unread_count ,
                    (select count(*) from ChatIsRead cir where cir.chat_no = c.chat_no  and cir.user_no = ? and cir.chat_room_no = ?) is_unread  
                    from Chat c 
                    join `User` u on u.user_no = c.user_no 
                    where c.chat_room_no = ? and
                        c.create_date BETWEEN 
                        (select user_chat_join_date from ChatRoom cr where chat_room_no = ? and user_no = ? )
                        and CURRENT_TIMESTAMP()
                    order by create_date ";
//            $sql = "select c.*, u.*
//                    from Chat c
//                    join `User` u on u.user_no = c.user_no
//                    where chat_room_no = ?
//                    order by create_date ";
//            (select절), ci.stored_file_name, ci.chat_image_no //(조인절)Left join ChatImage ci on ci.chat_no = c.chat_no
//                    Left join (select chat_no, read_date from ChatIsRead where user_no = ? ) cir
//                        on c.chat_no = cir.chat_no
//            $res3 = $chat->join('User', 'User.user_no = Chat.user_no')
//                ->join('ChatImage', 'ChatImage.chat_no = Chat.chat_no', 'left')
//                ->where('chat_room_no', $data['chat_room_no'])
////                ->where('group_no', $data['group_no'])
//                ->findAll();
            $res3 = $chat->db->query($sql, [ $data['user_no'], $data['chat_room_no'], $data['chat_room_no'], $data['chat_room_no'], $data['user_no']]);
            $res3 = $res3->getResultArray();
            //각 채팅에 이미지가 있으면 이미지를 넣어줌
            foreach ($res3 as $key => $item){
                if ($item['chat_type'] == '이미지') {
                    $res3[$key]['chat_image'] = $chatImage->where('chat_no', $item['chat_no'])->findAll(); //없으면 [] 빈배열반환
                } else {
                    $res3[$key]['chat_image'] = [];
                }
            }
            log_message("debug", "[group] chatRoomJoin \$res3: ". print_r($res3, true));

            $res4 = $chatImage->join('Chat', 'Chat.chat_no = ChatImage.chat_no')
                ->where('chat_room_no', $data['chat_room_no'])
                ->orderBy('ChatImage.create_date', 'desc')->findAll();

            $result = [
                'chat_room_info' => $res1[0],
                'chat_room_userL' => $res2,
                'chat_list' => $res3,
                'chat_room_image_list' => $res4
            ];

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $result,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
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


    // 채팅방 이미지전송 (GroupChatInnerFm)
    public function chatRoomUploadImages() : ResponseInterface
    {
        $user = new User();
        $chat = new Chat();
        $chatRoom = new ChatRoom();
        $chatRoomInfo = new ChatRoomInfo();
        $chatImage = new ChatImage();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Group] chatRoomUploadImages \$data: ". print_r($data, true));
        $imgData3 = $req->getFileMultiple('chat_image');
        log_message("debug", "[Group] chatRoomUploadImages \$getFileMultiple: ".print_r($imgData3, true));
//        $imgData = $req->getFile('gboard_image');
        $imgData = $req->getFiles();
        log_message("debug", "[Group] chatRoomUploadImages \$getFiles: ".print_r($imgData, true));


        $validationRule = [
            'chat_image' => [
                'label' => 'Image File',
                'rules' => 'uploaded[chat_image]'
                    . '|is_image[chat_image]'
                    . '|mime_in[chat_image,image/jpg,image/jpeg,image/gif,image/png,image/webp]'
                    . '|max_size[chat_image,1000]'
                    . '|max_dims[chat_image,1924,1924]',
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
            'chat_room_no' => $req->getVar('chat_room_no'),
            'chat_type' => '이미지',
            'create_date' => date('Y-m-d H:i:s'),
        ];

        try {
            $user->db->transStart();
            //모임을 만들고 반환된 모임 번호를 이용해 만든이를 모임장으로서 모임멤버로 추가시켜줌.
            $result = $chat->insert($data);
            log_message("debug", "[Group] chatRoomUploadImages \$result: ". print_r($result, true));

            //이미지파일을 ci 기본 upload 경로(writable/uploads)에 저장. 그후 경로 반환된걸로 db에 insert
            if ($imgData != null) {
                foreach ($imgData['chat_image'] as $img) { //받은 이미지파일배열을 반복문으로 돌려 ci기본 upload경로(writable/uploads)에 저장
                    if (! $img->hasMoved()) {
                        $originalName = $img->getClientName(); //원래 파일의 이름
                        $filePath =  $img->store(); //store는 기본경로에 날짜폴더를 생성하고 거기에 랜덤이름의 파일을 저장한다. 그 후 저장된 경로를 리턴
                        log_message("debug", "chatRoomUploadImages:\$originalName: ".print_r($originalName, true));
                        log_message("debug", "chatRoomUploadImages:\$filePath: ".print_r($filePath, true));
                        //                $fileName =  $img->getRandomName();
                        //                $img->move(WRITEPATH.'uploads', $fileName); //경로명, 저장될이름명(이이름으로저장됨) -- move는 tmp폴더에 임시로 받아진 사진파일을 지정한 경로에 파일명으로 저장한다. 그후 tmp폴더의 사진파일은 메소드종료와 동시에 삭제됨

                        //서버에 저장된 파일경로를 db에 저장함.
                        $chatImage->insert([
                            'chat_no' => $result,
                            'original_file_name' => $originalName,
                            'stored_file_name' => $filePath,
                            'file_size' => $img->getSize(),
                            'create_date' => date('Y-m-d H:i:s')
                        ]);
                    }
                }
            }

            //받아서 서버에 저장한 이미지 업로드 채팅의 정보를 다시 클라이언트로 반환해준다.
            $res1 = $chat->where('chat_no', $result)->findAll()[0];
            log_message("debug", "chatRoomUploadImages:\$res1: ".print_r($res1, true));
            $res1['chat_image'] = $chatImage->where('chat_no', $result)->findAll();

            log_message("debug", "chatRoomUploadImages:\$res1: ".print_r($res1, true));

            if ($user->db->transComplete()){
                $user->db->transCommit();
                return $res->setJSON([
                    "result" => $res1,
                    "msg" => "ok"
                ]);
            } else {
                $user->db->transRollback();
                return $res->setJSON([
                    "result" => $user->db->error(),
                    "msg" => "fail"
                ]);
            }
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



}