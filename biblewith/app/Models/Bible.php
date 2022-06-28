<?php

namespace App\Models;

class Bible extends \CodeIgniter\Model
{
    protected $table = 'bible_korHRV';
    protected $allowedFields = ['bible_no', 'book', 'chapter', 'verse', 'content' ];
    protected $primaryKey = 'bible_no';
}
