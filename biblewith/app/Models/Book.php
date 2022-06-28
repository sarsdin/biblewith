<?php

namespace App\Models;

class Book extends \CodeIgniter\Model
{
    protected $table = 'book';
    protected $allowedFields = ['book_no', 'book_name', 'book_category' ];
    protected $primaryKey = 'book_no';
}

