<?php

namespace App\Models;

class ChatIsRead extends \CodeIgniter\Model
{
    protected $table = 'ChatIsRead';
    protected $allowedFields = [
        'chat_read_no',
        'chat_no',
        'user_no',
        'read_date'
    ];
    protected $primaryKey = 'chat_read_no';
}
