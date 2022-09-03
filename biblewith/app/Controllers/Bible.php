<?php

namespace App\Controllers;

use App\Models\Book;
use App\Models\HighLight;
use App\Models\Note;
use App\Models\NoteVerse;
use App\Models\User;
use CodeIgniter\Database\Exceptions\DataException;
use CodeIgniter\HTTP\ResponseInterface;

class Bible extends \CodeIgniter\Controller
{

    //성경 일독 로드
    public function getTodayLang() : ResponseInterface
    {
        $book = new Book();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getVar();
//        log_message("debug", "[Bible] getBookList \$data: ". print_r($data, true));

//        $랜덤구절번호생성 = sprintf("%06d", mt_rand(1, 999999));
        $sql = " select * from bible_korHRV bkh join book b on bkh.book = b.book_no  where book_name in ('시편','잠언', '전도서', '아가' )
                or (bkh.bible_no BETWEEN 23146 and 31102) 
                order by RAND() LIMIT 1  ";

        try {
            $book->db->transStart();
            //book_no가 아닌 book으로 해야되는 이유: gson serialize 할때 Dto에 맵핑시 어디서는 book으로 쓰이고 어디서는 book_no로 쓰여서
            //헷갈리고 어디서는 맵핑도 안된다. 애초에 book_no가 아닌 book으로 통일했으면 dto에서 헷갈릴 일도 없지..ㅠㅠ
//            $sql = " select book_no book, book_name, book_category from book ";
            $result = $book->db->query($sql);
            $result = $result->getResultArray();
//            log_message("debug", "[Bible] getTodayLang \$result: ". print_r($result, true));

            if ($book->db->transComplete()) {
                $book->db->transCommit();
                return $res->setJSON([
                    "result" => $result[0],
                    "msg" => "ok"
                ]); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                $book->db->transRollback();
                log_message("debug", "[Bible] getTodayLang \$book->db->error(): ". print_r($book->db->error(), true));
                return $res->setJSON([
                    "result" => $book->db->error(),
                    "msg" => "fail"
                ]);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }



///////////////////////////////////////////////////////////////////////////////////성경 시작

    // 성경 책 이름 정보 반환
    public function getBookList() : ResponseInterface
    {
        $book = new Book();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getVar();
//        log_message("debug", "[Bible] getBookList \$data: ". print_r($data, true));

        try {
            //book_no가 아닌 book으로 해야되는 이유: gson serialize 할때 Dto에 맵핑시 어디서는 book으로 쓰이고 어디서는 book_no로 쓰여서
            //헷갈리고 어디서는 맵핑도 안된다. 애초에 book_no가 아닌 book으로 통일했으면 dto에서 헷갈릴 일도 없지..ㅠㅠ
            $sql = " select book_no book, book_name, book_category from book ";
            $result = $book->db->query($sql);
            $result = $result->getResultArray();
//            log_message("debug", "[Bible] getBookList \$result: ". print_r($result, true));

            if ($result != null) {
                return $res->setJSON($result); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Bible] getBookList \$result is null: ". print_r($result, true));
                return $res->setJSON($result);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 장 정보 반환
    public function getChapterList() : ResponseInterface
    {
        $book = new Book();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Bible] getChapterList \$data: ". print_r($data, true));

//        $nameBinding = [
//            'book' => $data['book'],
//        ];
        try {
//            $result = $bible->where('book', $data['book'])->findColumn('chapter');
            $sql = " SELECT DISTINCT  chapter from bible_korHRV where book = ? ";
            $result = $bible->db->query($sql, $data['book']);
            $result = $result->getResultArray();
//            log_message("debug", "[Bible] getChapterList \$result: ". print_r($result, true));


            if ($result != null) {
                return $res->setJSON($result); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Bible] getChapterList \$result is null: ". print_r($result, true));
                return $res->setJSON($result);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }


    }


    // 절 정보 반환
    public function getVerseList() : ResponseInterface
    {
        $book = new Book();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Bible] getVerseList \$data: ". print_r($data, true));

//        $nameBinding = [
//            'book' => $data['book'],
//        ];
        try {
//            $result = $bible->where('book', $data['book'])->findColumn('chapter');
            $sql = " SELECT * from bible_korHRV where book = ? and chapter = ? ";
            $result = $bible->db->query($sql, [$data['book'], $data['chapter']]);
            $result = $result->getResultArray();
//            log_message("debug", "[Bible] getVerseList \$result: ". print_r($result, true));

            if ($result != null) {
                return $res->setJSON($result); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Bible] getVerseList \$result is null: ". print_r($result, true));
                return $res->setJSON($result);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }


    }


    // 책 검색 정보 반환
    public function getSearchBookList() : ResponseInterface
    {
        $book = new Book();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Bible] getSearchBookList \$data: ". print_r($data, true));

//        $nameBinding = [
//            'book' => $data['book'],
//        ];
        try {
//            $result = $bible->where('book', $data['book'])->findColumn('chapter');
            $sql = " SELECT * from bible_korHRV where book_name like concat_ws( ? , '%', '%')  ";
            $result = $book->db->query($sql, $data['book_name']);
            $result = $result->getResultArray();
            log_message("debug", "[Bible] getSearchBookList \$result: ". print_r($result, true));

            if ($result != null) {
                return $res->setJSON($result); //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문

            } else {
                log_message("debug", "[Bible] getSearchBookList \$result is null: ". print_r($result, true));
                return $res->setJSON($result);
            }

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }


    }


    // 유저 하이라이트 정보 반환
    public function getHlList() : ResponseInterface
    {
        $book = new Book();
        $hl = new HighLight();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar();
        log_message("debug", "[Bible] getHlList \$data: ". print_r($data, true));

        $sql = " select * from bible_korHRV b 
                    join HighLight hl on b.bible_no = hl.bible_no 
                    join book k on b.book = k.book_no 
                    where user_no = ? ";
        try {
            $result = $hl->join('bible_korHRV', 'bible_korHRV.bible_no = HighLight.bible_no')
                ->join('book', 'bible_korHRV.book = book.book_no')
                ->where("user_no", $data['user_no'])->orderBy('highlight_date', 'DESC')->findAll();
//            $result = $hl->db->query($sql, $data['user_no']);
//            $result = $result->getResultArray();
            log_message("debug", "[Bible] getHlList \$result: ". print_r($result, true));

            return $res->setJSON($result);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 유저 하이라이트 업데이트 및 정보 반환 - insert, update, select 동시에 진행됨...
    public function getHlUpdate() : ResponseInterface
    {
        $book = new Book();
        $hl = new HighLight();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJSON(true); // $data3을 연관배열형태로 바꾼 값이 넘어옴. $array = json_decode(json_encode($data3), true);을 수행한 값이랑 같음.
//        $data2 = $req->getBody(); //리스트까지도 오긴하지만, 일렬로 쭉 붙어서 지저분한 형태로 데이터가 넘어옴. @FormUrlEncoded & @Field 조합
//        $data3 = $req->getVar(); //리스트는 마지막 인덱스의 것만 넘어옴. 그리고, 배열안의 객체는 stdClass 형태로 가져옴. 타입이 배열아닌 객체임.
        log_message("debug", "[Bible] getHlUpdate \$data: ". print_r($data, true));
//        log_message("debug", "[Bible] getHlUpdate \$data2: ". print_r($data2, true));
//        log_message("debug", "[Bible] getHlUpdate \$data3: ". print_r($data3, true));

        try {
            $upData = $data['tmpHighL'];
            $delData = $data['delHighL'];
            $dbData = $hl->where('user_no', $data['user_no'])->findAll(); //db 유저 하이라이트 배열 가져옴

            //signalDel 이 true이면 삭제처리 진행
            if($data['signalDel'] == 1){
                $hl->where('user_no', $data['user_no'])->whereIn('bible_no', $delData)->delete();
                $result = $hl->join('bible_korHRV', 'bible_korHRV.bible_no = HighLight.bible_no')
                    ->join('book', 'bible_korHRV.book = book.book_no')
                    ->where("user_no", $data['user_no'])->findAll();
//            log_message("debug", "[Bible] getHlUpdate \$result: ". print_r($result, true));

                return $res->setJSON($result);
            }

            $hl->db->transStart();

            foreach($upData as $key => $value ) { //list의 key는 숫자임. 0번부터
                $isUpdate = false;
                foreach($dbData as $key2 => $value2 ) {
                    if($value['bible_no'] == $value2['bible_no']){ //db의 현재유저에 이미 존재하는 하이라이트라면 색깔만 update해줌.
                        $hl->where('user_no', $data['user_no'])
                            ->where('bible_no', $value['bible_no'])
                            ->set([ 'highlight_color' => $value['highlight_color'],
                                'highlight_date' => date('Y-m-d H:i:s')
                            ])
                            ->update();
                        $isUpdate = true;
                        break 1; //1은 중첩 깊이를 말함. 기본값임. 2면 제일위의 foreach 문까지도 한번에 중지함.
                    }
                    log_message("debug", "[Bible] getHlUpdate \$isUpdate: $isUpdate". print_r($isUpdate, true));
                }
                //update 로직에서 update true이면 $upData의 해당 하이라이트항목은 insert 하지 않고,
                //update 작업이 이루어지지 않은 false 라면 새로운 하이라이트로 판단하고 insert 해야함.
                if(!$isUpdate){
                    $hl->insert([
                        'user_no' => $data['user_no'],
                        'bible_no' => $value['bible_no'],
                        'highlight_date' => date('Y-m-d H:i:s'),
                        'highlight_color' => $value['highlight_color']
                    ]);
                }
            }
            $transRes = $hl->db->transComplete();
            /*if($transRes){
                $hl->db->transCommit();
            } else {
                $hl->db->transRollback();
            }*/

            //insert, update 완료된 유저 하이라이트 목록을 클라이언트에 보내줌.
            $result = $hl->join('bible_korHRV', 'bible_korHRV.bible_no = HighLight.bible_no')
                ->join('book', 'bible_korHRV.book = book.book_no')
                ->where("user_no", $data['user_no'])->findAll();
//            log_message("debug", "[Bible] getHlUpdate \$result: ". print_r($result, true));

            return $res->setJSON($result);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException | \Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 유저 하이라이트 삭제 및 정보 반환 - delete, select 동시에 진행됨...
    public function getHlDelete() : ResponseInterface
    {
        $book = new Book();
        $hl = new HighLight();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJSON(true); // $data3을 연관배열형태로 바꾼 값이 넘어옴. $array = json_decode(json_encode($data3), true);을 수행한 값이랑 같음.
        log_message("debug", "[Bible] getHlUpdate \$data: ". print_r($data, true));

        try {
            $delData = $data['delHighL'];

            $hl->where('user_no', $data['user_no'])->whereIn('bible_no', $delData)->delete();
            $result = $hl->join('bible_korHRV', 'bible_korHRV.bible_no = HighLight.bible_no')
                ->join('book', 'bible_korHRV.book = book.book_no')
                ->where("user_no", $data['user_no'])->findAll();
            log_message("debug", "[Bible] getHlUpdate \$result: ". print_r($result, true));

            return $res->setJSON($result);

        } catch (\ReflectionException | DataException | \Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }


    // 유저 노트 추가
    public function getNoteAdd() : ResponseInterface
    {
        $note = new Note();
        $noteVerse = new NoteVerse();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJSON(true); // $data3을 연관배열형태로 바꾼 값이 넘어옴. $array = json_decode(json_encode($data3), true);을 수행한 값이랑 같음.
        log_message("debug", "[Bible] getNoteAdd \$data: ". print_r($data, true));

        try {
            $noteRes = $note->insert([
                'user_no' => $data['user_no'],
                'note_content' => $data['note_content'],
                'note_date' => date('Y-m-d H:i:s')
            ], true);
            foreach ($data['note_verseL'] as $key => $value ) {
                $data['note_verseL'][$key]['note_no'] = $noteRes;
            }
            $result = $noteVerse->insertBatch($data['note_verseL']);

            log_message("debug", "[Bible] getNoteAdd \$data['note_verseL']: ". print_r($data['note_verseL'], true));

            return $res->setJSON(['result' => $result]);

        } catch (\ReflectionException | DataException | \Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 유저 노트 수정
    public function getNoteUpdate() : ResponseInterface
    {
        $note = new Note();
        $noteVerse = new NoteVerse();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJSON(true); // $data3을 연관배열형태로 바꾼 값이 넘어옴. $array = json_decode(json_encode($data3), true);을 수행한 값이랑 같음.
        log_message("debug", "[Bible] getNoteUpdate \$data: ". print_r($data, true));
        try {
            $result = $note->set($data)->update($data['note_no']); //특정 컬럼만 수정할때는 set()메소드를 반드시 선언해서 지정해야한다.
            log_message("debug", "[Bible] getNoteUpdate \$data['note_verseL']: ". print_r($result, true));
            return $res->setJSON(['result' => $result]);

        } catch (\ReflectionException | DataException | \Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 유저 노트 삭제
    public function deleteNote() : ResponseInterface
    {
        $note = new Note();
        $noteVerse = new NoteVerse();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getVar(); // $data3을 연관배열형태로 바꾼 값이 넘어옴. $array = json_decode(json_encode($data3), true);을 수행한 값이랑 같음.
        log_message("debug", "[Bible] deleteNote \$data: ". print_r($data, true));
        try {
            $result = $note->delete($data['note_no']);
            log_message("debug", "[Bible] deleteNote \$result: ". print_r($result, true));
            return $res->setJSON(['result' => $result]);

        } catch (\ReflectionException | DataException | \Exception $e){
            return $res->setJSON($e->getMessage());
        }
    }

    // 유저 노트 정보 반환
    public function getNoteList() : ResponseInterface
    {
        $note = new Note();
        $noteVerse = new NoteVerse();
        $bible = new \App\Models\Bible();
        $req = $this->request;
        $res = $this->response;
        $data = $req->getJson(true);
        $data = $req->getVar();
        log_message("debug", "[Bible] getNoteList \$data: ". print_r($data, true));

        try {
            $noteRes = $note->where("user_no", $data['user_no'])->orderBy('note_date', 'DESC')->findAll();
            foreach ($noteRes as $key => $item ) {
                $noteRes[$key]['note_verseL'] = $noteVerse
                    ->join('bible_korHRV', 'bible_korHRV.bible_no = NoteVerse.bible_no')
                    ->where('note_no', $item['note_no'])->findAll();
            }

            log_message("debug", "[Bible] getNoteList \$result: ". print_r($noteRes, true));

            return $res->setJSON($noteRes);
            //배열로 반환되면 클라이언트에서 Dto객체로 변환이 안된다. Dto는 Object 타입이기때문. arrayL라면 arrayL 로 변환되어야함.

        } catch (\ReflectionException | DataException $e){
            return $res->setJSON($e->getMessage());
        }
    }





}