<?php

namespace App\Controllers;

use App\Models\Book;
use App\Models\User;
use CodeIgniter\Database\Exceptions\DataException;
use CodeIgniter\HTTP\ResponseInterface;

class Bible extends \CodeIgniter\Controller
{


    // 성경 책 이름 정보 반환
    public function getBookList() : ResponseInterface
    {
        $book = new Book();
        $req = $this->request;
        $res = $this->response;
//        $data = $req->getVar();
//        log_message("debug", "[Bible] getBookList \$data: ". print_r($data, true));

        try {
            $result = $book->findAll();
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
            $sql = " SELECT verse, content from bible_korHRV where book = ? and chapter = ? ";
            $result = $bible->db->query($sql, [$data['book'], $data['chapter']]);
            $result = $result->getResultArray();
            log_message("debug", "[Bible] getVerseList \$result: ". print_r($result, true));

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





}