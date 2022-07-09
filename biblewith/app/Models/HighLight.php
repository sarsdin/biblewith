<?php

namespace App\Models;

class HighLight extends \CodeIgniter\Model
{
    protected $table = 'HighLight';
    protected $allowedFields = ['user_no', 'bible_no', 'highlight_date', 'highlight_color', 'highlight_no' ];
    protected $primaryKey = 'highlight_no';
}
